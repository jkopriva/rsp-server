/*******************************************************************************
 * Copyright (c) 2007 - 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ssp.server.wildfly.servertype.launch;

import org.jboss.tools.ssp.server.spi.servertype.IServer;

public class JBoss72Eap61DefaultLaunchArguments extends
		JBoss71DefaultLaunchArguments {
	public JBoss72Eap61DefaultLaunchArguments(IServer s) {
		super(s);
	}
	public String getStartDefaultVMArgs() {
		return super.getStartDefaultVMArgs() 
				+ "-Dorg.jboss.logmanager.nocolor=true "; //$NON-NLS-1$
	}
	protected String getMemoryArgs() {
		return "-Xms1024m -Xmx1024m -XX:MaxPermSize=256m "; //$NON-NLS-1$
	}
}
