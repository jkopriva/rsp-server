/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.wildfly.servertype.launch;

import org.jboss.tools.rsp.server.spi.servertype.IServer;

public class JBossEAP60LaunchArgs extends JBoss71DefaultLaunchArguments {

	public JBossEAP60LaunchArgs(IServer rt) {
		super(rt);
	}

	@Override
	protected String getMemoryArgs() {
		return "-Xms1303m -Xmx1303m -XX:MaxPermSize=256m "; //$NON-NLS-1$
	}
}