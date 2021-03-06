/*******************************************************************************
 * Copyright (c) 2013, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.ui.internal.wizards;

import java.util.List;

import org.eclipse.cft.server.core.AbstractApplicationDelegate;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Delegate that provides Application deployment wizard pages through the
 * extension point:
 * 
 * org.eclipse.cft.server.ui.applicationWizard
 * 
 * <p/>
 * The wizard delegate may correspond got an AbstractApplicationDelegate, with the
 * difference that the wizard delegate provides UI when deploying the
 * application via the application deployment wizard. On the other hand, the
 * AbstractApplicationDelegate is a core level delegate that contains API necessary to
 * push the application to a CF server. The need for two separate delegates is
 * due to the core and UI components of the plugin being separate.
 * 
 * 
 * @see AbstractApplicationDelegate
 * 
 */
public interface IApplicationWizardDelegate {

	/**
	 * Provide a list of pages for the application deployment wizard that would
	 * replace the list of default pages. The default list of pages are not yet
	 * set in the wizard when this method is called by the framework. The
	 * returned list of pages will be set in the wizard via the wizard's
	 * addPages(...) API. Consequently, any pages that are returned by this
	 * method can be assumed to have a reference to the wizard via the
	 * getWizard(..) API in the IWizardPage when the page controls are created
	 * by the wizard.
	 * <p/>
	 * The descriptor contains values that need to be set in order for an
	 * application to be pushed to a Cloud Foundry server. Only one instance of
	 * the descriptor exists per wizard session, and it is shared amongst all
	 * the wizard pages. Values should be set directly in this descriptor in
	 * order for the wizard to be completed.
	 * 
	 * 
	 * @param descriptor shared descriptor that contains information necessary
	 * to push an application. Only one instance of the descriptor exists per
	 * wizard session, and it is shared amongst all the wizard pages
	 * @param server the Cloud Foundry server instance where the
	 * application will be pushed to.
	 * @param module representing the application that will be
	 * pushed to the Cloud Foundry server
	 * @return List of pages that should be set, which replace the default
	 * pages. Return null or empty list if the default wizard pages should be
	 * used.
	 */
	public List<IWizardPage> getWizardPages(ApplicationWizardDescriptor descriptor, IServer server,
			IModule module);

}
