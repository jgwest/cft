/*******************************************************************************
 * Copyright (c) 2014, 2016 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.client;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;

/**
 * An operation performed in a {@link CloudFoundryServerBehaviour} target Cloud
 * space on a target IModule
 */
public abstract class BehaviourOperation implements ICloudFoundryOperation {

	private final CloudFoundryServerBehaviour behaviour;

	private final IModule module;

	public BehaviourOperation(CloudFoundryServerBehaviour behaviour, IModule module) {
		this.behaviour = behaviour;
		this.module = module;
	}

	public String getMessage() {
		return null;
	}

	public CloudFoundryServerBehaviour getBehaviour() {
		return behaviour;
	}

	public IModule getModule() {
		return module;
	}

	protected CloudFoundryApplicationModule getCloudModule() {
		try {
			return CloudServerUtil.getCloudFoundryApplicationModule(module,
					behaviour.getCloudFoundryServer().getServer());
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}
}
