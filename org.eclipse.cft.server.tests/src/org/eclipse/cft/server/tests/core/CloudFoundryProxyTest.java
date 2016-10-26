/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * and the Apache License v2.0 is available at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.tests.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryClientFactory;
import org.eclipse.cft.server.tests.sts.util.ProxyHandler;
import org.eclipse.cft.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public class CloudFoundryProxyTest extends AbstractAsynchCloudTest {

	public static final String VALID_V1_HTTP_URL = "http://api.cloudfoundry.com";

	public static final String VALID_V1_HTTPS_URL = "https://api.cloudfoundry.com";

	public void testInvalidProxyJavaCFClient() throws Exception {

		final boolean[] ran = { false };
		new ProxyHandler("invalid.proxy.test", 8080) {

			@Override
			protected void handleProxyChange() {

				// Create app. Should fail
				CloudFoundryOperations client = null;
				Exception error = null;
				try {
					List<String> uris = new ArrayList<String>();
					uris.add("test-proxy-upload.cloudfoundry.com");

					// Do a direct client test with the proxy settings
					client = getTestFixture().createExternalClient();
					client.createApplication("test", new Staging(), 128, uris, new ArrayList<String>());
					fail("Expected ResourceAccessException due to invalid proxy configuration");
				}
				catch (Exception e) {
					ran[0] = true;
					error = e;
				}

				assertTrue("Expected access exception ", error instanceof IllegalArgumentException);

				assertNull("Expected no client due to invalid proxy", client);
			}

		}.run();

		assertTrue(ran[0]);
	}

	public void testInvalidProxyThroughServerInstance() throws Exception {

		// NOTE: As of CF 1.6.0 (Cloud Foundry Java client 1.0.2), proxy changes
		// REQUIRE a client change. They can no longer be changed during the
		// same
		// client session used by the CF server instance. Therefore, proxy
		// changes
		// require a new connection.

		// Verify that connection and operations can be performed without the
		// proxy change
		String prefix = "InvalidProxyServerInstance";
		createWebApplicationProject();

		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp, harness.getDefaultBuildpack());

		final String appName = appModule.getDeployedApplicationName();

		final boolean[] ran = { false };

		new ProxyHandler("invalid.proxy.test", 8080) {

			@Override
			protected void handleProxyChange() throws CoreException {

				IProxyService proxyService = getProxyService();
				try {
					// Reset the client to use the new proxy settings
					connectClient();

					IModule[] modules = server.getModules();
					// Should fail, as its now going through invalid proxy
					serverBehavior.stopModule(modules, null);

					fail("Expected invalid.proxy.test failure");

				}
				catch (Exception error) {
					assertTrue("Expected access exception ", error instanceof IllegalArgumentException);
					ran[0] = true;
				}

				// Restore proxy settings and try again
				proxyService.setSystemProxiesEnabled(getOriginalSystemProxiesEnabled());
				proxyService.setProxiesEnabled(getOriginalProxiesEnabled());
				proxyService.setProxyData(getOriginalProxyData());

				// Note that connecting again launches a refresh job
				// therefore wait until refresh modules is completed before
				// stopping it
				ModulesRefreshListener listener = getModulesRefreshListener(null, cloudServer,
						CloudServerEvent.EVENT_UPDATE_COMPLETED);
				connectClient();
				assertModuleRefreshedAndDispose(listener, CloudServerEvent.EVENT_UPDATE_COMPLETED);

				IModule[] modules = server.getModules();

				serverBehavior.stopModule(modules, null);

				int moduleState = server.getModuleState(modules);
				assertEquals(IServer.STATE_STOPPED, moduleState);
				CloudApplication cloudApplication = getUpdatedApplication(appName);
				assertEquals(cloudApplication.getState(), CloudApplication.AppState.STOPPED);
			}

		}.run();

		assertTrue(ran[0]);
	}

	public void testNoProxySetHTTP() throws Exception {

		new ProxyHandler(null, -1, false, IProxyData.HTTP_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTP_URL));
					assertNull(configuration);

					// verify it is null for https as well
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTPS_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudErrorUtil.toCoreException(e);
				}

			}
		};
	}

	public void testNoProxySetHTTPS() throws Exception {

		new ProxyHandler(null, -1, false, IProxyData.HTTPS_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTPS_URL));
					assertNull(configuration);

					// verify it is null for https as well
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTP_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudErrorUtil.toCoreException(e);
				}

			}
		}.run();
	}

	public void testProxySetDirectHTTPS() throws Exception {
		// Direct provider means not using proxy settings even if they are set.
		// To set direct provider, disable proxies even when set
		boolean enableProxies = false;
		new ProxyHandler("invalid.proxy.test", 8080, enableProxies, IProxyData.HTTPS_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTPS_URL));
					assertNull(configuration);

					// verify it is null for https as well
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTP_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudErrorUtil.toCoreException(e);
				}

			}
		}.run();
	}

	public void testProxyDirectSetHTTP() throws Exception {
		// Direct provider means not using proxy settings even if they are set.
		// To set direct provider, disable proxies even when set
		boolean enableProxies = false;
		new ProxyHandler("invalid.proxy.test", 8080, enableProxies, IProxyData.HTTP_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTP_URL));
					assertNull(configuration);

					// verify it is null for https as well
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTPS_URL));

					assertNull(configuration);

				}
				catch (MalformedURLException e) {
					throw CloudErrorUtil.toCoreException(e);
				}

			}
		}.run();
	}

	public void testProxyManualSetHTTP() throws Exception {

		boolean setProxy = true;
		new ProxyHandler("invalid.proxy.test", 8080, setProxy, IProxyData.HTTP_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTP_URL));
					assertNotNull(configuration);
					assertTrue(configuration.getProxyHost().equals("invalid.proxy.test"));
					assertTrue(configuration.getProxyPort() == (8080));

					// verify it is null for https, since proxy lookup should
					// match the specified
					// protocol in the given URL (i.e. HTTP proxy -> HTTP URL,
					// HTTPS proxy -> HTTPS URL)
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTPS_URL));

					// Need to add this code in case the server has a proxy set
					// for either HTTP or HTTPS
					if (configuration != null) {
						assertTrue(!configuration.getProxyHost().equals("invalid.proxy.text"));
					}

				}
				catch (MalformedURLException e) {
					throw CloudErrorUtil.toCoreException(e);
				}

			}
		}.run();
	}

	public void testProxySetManualHTTPS() throws Exception {
		boolean setProxy = true;
		new ProxyHandler("invalid.proxy.test", 8080, setProxy, IProxyData.HTTPS_PROXY_TYPE) {

			@Override
			protected void handleProxyChange() throws CoreException {
				try {
					HttpProxyConfiguration configuration = CloudFoundryClientFactory
							.getProxy(new URL(VALID_V1_HTTPS_URL));
					assertNotNull(configuration);
					assertTrue(configuration.getProxyHost().equals("invalid.proxy.test"));
					assertTrue(configuration.getProxyPort() == (8080));

					// verify it is null for http, since proxy lookup should
					// match the specified
					// protocol in the given URL (i.e. HTTP proxy -> HTTP URL,
					// HTTPS proxy -> HTTPS URL)
					configuration = CloudFoundryClientFactory.getProxy(new URL(VALID_V1_HTTP_URL));

					// Need to add this code in case the server has a proxy set
					// for either HTTP or HTTPS
					if (configuration != null) {
						assertTrue(!configuration.getProxyHost().equals("invalid.proxy.text"));
					}

				}
				catch (MalformedURLException e) {
					throw CloudErrorUtil.toCoreException(e);
				}

			}
		}.run();
	}

}
