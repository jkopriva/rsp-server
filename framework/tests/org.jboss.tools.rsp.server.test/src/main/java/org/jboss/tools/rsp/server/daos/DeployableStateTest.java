/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.api.dao.ServerHandle;
import org.jboss.tools.rsp.api.dao.ServerType;
import org.junit.Before;
import org.junit.Test;

public class DeployableStateTest {

	private DeployableState state;
	private ServerHandle serverRef;
	
	private static final String LABEL = "papa-smurf";
	private static final String PATH = "/in/da/house";

	@Before	
	public void before() {
		this.serverRef = new ServerHandle("id", new ServerType("typeId", "name", "Desc"));
		DeployableReference reference = new DeployableReference(LABEL, PATH);
		this.state = new DeployableState(serverRef, reference, 
				ServerManagementAPIConstants.STATE_UNKNOWN, ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN); 
	}

	@Test
	public void equalIfAllAreEqual() {
		assertEquals(state, new DeployableState(serverRef,state.getReference(), state.getState(), state.getPublishState()));
	}

	@Test
	public void notEqualIfReferenceDiffers() {
		assertNotEquals(state, new DeployableState(serverRef, new DeployableReference("some-label", "some-path"),
				state.getState(), state.getPublishState()));
	}

	@Test
	public void notEqualIfStateDiffers() {
		assertNotEquals(state, new DeployableState(serverRef, state.getReference(), 
				ServerManagementAPIConstants.STATE_STOPPING, state.getPublishState()));
	}

	@Test
	public void notEqualIfPublishStateDiffers() {
		assertNotEquals(state, new DeployableState(serverRef, state.getReference(), 
				state.getState(), ServerManagementAPIConstants.PUBLISH_STATE_ADD));
	}
}
