/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.server.model.internal.publishing;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.DeployableReference;
import org.jboss.tools.rsp.api.dao.DeployableState;
import org.jboss.tools.rsp.eclipse.core.runtime.IStatus;
import org.jboss.tools.rsp.eclipse.core.runtime.Status;
import org.jboss.tools.rsp.eclipse.osgi.util.NLS;
import org.jboss.tools.rsp.server.ServerCoreActivator;
import org.jboss.tools.rsp.server.model.AbstractServerDelegate;
import org.jboss.tools.rsp.server.spi.filewatcher.FileWatcherEvent;
import org.jboss.tools.rsp.server.spi.filewatcher.IFileWatcherEventListener;
import org.jboss.tools.rsp.server.spi.filewatcher.IFileWatcherService;
import org.jboss.tools.rsp.server.spi.servertype.IDeployableResourceDelta;
import org.jboss.tools.rsp.server.spi.servertype.IServerPublishModel;

public class ServerPublishStateModel implements IServerPublishModel, IFileWatcherEventListener {

	private final Map<String, DeployableState> states;
	private final Map<String, Map<String,Object>> deploymentOptions;
	private final Map<String, DeployableDelta> deltas = new HashMap<>();
	
	private AbstractServerDelegate delegate;
	private IFileWatcherService fileWatcher;
	private int publishState = AbstractServerDelegate.PUBLISH_STATE_UNKNOWN;
	
	public ServerPublishStateModel(AbstractServerDelegate delegate, IFileWatcherService fileWatcher) {
		this.delegate = delegate;
		this.fileWatcher = fileWatcher;
		this.states = new LinkedHashMap<>();
		this.deploymentOptions = new LinkedHashMap<>();
	}

	@Override
	public void initialize(List<DeployableReference> references) {
		for( DeployableReference reference : references ) {
			addDeployableImpl(reference, ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN);
		}
		updateServerPublishStateFromDeployments();
		fireState();
	}
	
	private void addDeployableImpl(DeployableReference reference, int publishState) {
		DeployableState deployableState = 
				createDeployableState(reference, publishState, ServerManagementAPIConstants.STATE_UNKNOWN);
		
		String key = getKey(reference);
		getStates().put(key, deployableState);
		deploymentOptions.put(getKey(reference), reference.getOptions());

		registerFileWatcher(reference);
	}

	private DeployableState createDeployableState(DeployableReference reference, int publishState, int state) {
		DeployableState deployableState = new DeployableState();
		deployableState.setPublishState(publishState);
		deployableState.setState(state);
		deployableState.setReference(reference);
		deployableState.setServer(delegate.getServerHandle());
		return deployableState;
	}
	
	private void registerFileWatcher(DeployableReference reference) {
		File f = new File(reference.getPath());
		boolean recursive = f.exists() && f.isDirectory();
		
		String path = reference.getPath();
		if( fileWatcher != null ) {
			fileWatcher.addFileWatcherListener(new File(path).toPath(), this, recursive);
		}
	}

	/**
	 * Adds the given deployable to this model.
	 * 
	 * @param withOptions the deployable to add.
	 */
	@Override
	public IStatus addDeployable(DeployableReference withOptions) {
		if (contains(withOptions)) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, IStatus.ERROR, 
					NLS.bind("Could not add deploybale with path {0}: it already exists.", 
							getKey(withOptions)), null);
		}
		addDeployableImpl(withOptions, ServerManagementAPIConstants.PUBLISH_STATE_ADD);
		updateServerPublishStateFromDeployments();
		fireState();
		return Status.OK_STATUS;
	}

	@Override
	public boolean contains(DeployableReference reference) {
		return getStates().containsKey(getKey(reference));
	}

	@Override
	public IStatus removeDeployable(DeployableReference reference) {
		DeployableState ds = getStates().get(getKey(reference));
		if (ds == null) {
			return new Status(IStatus.ERROR, ServerCoreActivator.BUNDLE_ID, IStatus.ERROR, 
					NLS.bind("Could not remove deploybale with path {0}: it doesn't exist", getKey(reference)),
							null);
		}
		if (ds.getPublishState() == ServerManagementAPIConstants.PUBLISH_STATE_ADD) {
			// It hasn't been added / published yet, so we can remove it immediately
			deployableRemoved(reference);
		}
		ds.setPublishState(ServerManagementAPIConstants.PUBLISH_STATE_REMOVE);
		String path = reference.getPath();
		if (fileWatcher != null) {
			fileWatcher.removeFileWatcherListener(new File(path).toPath(), this);
		}
		updateServerPublishStateFromDeployments();
		fireState();
		return Status.OK_STATUS;
	}

	protected String getKey(DeployableReference reference) {
		if (reference == null) {
			return null;
		} 
		return reference.getPath();
	}
	
	@Override
	public void deployableRemoved(DeployableReference reference) {
		String k = getKey(reference);
		getStates().remove(k);
		deploymentOptions.remove(k);
	}

	@Override
	public List<DeployableState> getDeployableStates() {
		return new ArrayList<>(getStates().values());
	}

	@Override
	public DeployableState getDeployableState(DeployableReference reference) {
		DeployableState ds = getStates().get(getKey(reference));
		if (ds == null) {
			return null;
		}
		return createDeployableState(reference, ds.getPublishState(), ds.getState());
	}

	/**
	 * for testing purposes
	 */
	protected Map<String, DeployableState> getStates() {
		return states;
	}

	/**
	 * for testing purposes
	 */
	protected Map<String, DeployableDelta> getDeltas() {
		return deltas;
	}

	@Override
	public void setDeployablePublishState(DeployableReference reference, int publishState) {
		DeployableState ds = getDeployableState(reference);
		if (ds == null) {
			return;
		}
		DeployableState next = createDeployableState(reference, publishState, ds.getState());
		String key = getKey(reference);
		getStates().put(key, next);
		if( publishState == ServerManagementAPIConstants.PUBLISH_STATE_NONE) {
			clearDelta(key);
		}
		updateServerPublishStateFromDeployments();
	}

	private void clearDelta(String key) {
		DeployableDelta delta2 = getDeltas().get(key);
		if( delta2 != null ) {
			delta2.clear();
		}
	}

	@Override
	public void setDeployableState(DeployableReference reference, int runState) {
		DeployableState ds = getStates().get(getKey(reference));
		if (ds == null) {
			return;
		}
		DeployableState next = createDeployableState(reference, ds.getPublishState(), runState);
		getStates().put(getKey(reference), next);
	}

	/*
	 * If a path matching one of our deployments has been modified,
	 * created, or deleted, we should respond to this. However, most 
	 * cases will not require us to do much of anything. 
	 * 
	 *  If the deployment is currently set to be 'added', we should not make any changes
	 *  to the publish state, since the next publish will do the full add as expected.
	 *  
	 *  If the deployment is currently set to be 'removed', we should not make any 
	 *  changes to the publish state, since the next publish will remove the deployment.
	 *  
	 *  If the deployment is currently set to be 'incremental', 
	 *  no change is needed. It's already marked as requiring a publish. 
	 *  
	 *  If the deployment is currently set to be 'full',
	 *  no change is needed. It's already marked as requiring a publish.
	 *  
	 *  If the deployment is currently set to 'unknown', 
	 *  we should not make any change, so the delegate knows the state is still uncertain.
	 *  
	 *  So only if the deployment is currently set to 'none' do we know 
	 *  that we should now mark it as requiring an incremental publish. 
	 * 
	 */
	@Override
	public void fileChanged(FileWatcherEvent event) {
		Path affected = event.getPath();
		List<DeployableState> ds = new ArrayList<>(getStates().values());
		boolean changed = false;
		for( DeployableState d : ds ) {
			Path deploymentPath = new File(d.getReference().getPath()).toPath();
			if( affected.startsWith(deploymentPath)) {
				int currentPubState = d.getPublishState();
				if( currentPubState == ServerManagementAPIConstants.PUBLISH_STATE_NONE) {
					d.setPublishState(ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL);
					changed = true;
				}
				if( currentPubState == ServerManagementAPIConstants.PUBLISH_STATE_NONE ||
						currentPubState == ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL) {
					registerSingleDelta(event, d.getReference());
				}
			}
		}
		updateServerPublishStateFromDeployments();
		if( changed ) 
			fireState();
	}

	private void registerSingleDelta(FileWatcherEvent event, DeployableReference reference) {
		String key = getKey(reference);
		DeployableDelta dd = getDeltas().computeIfAbsent(key, k ->  new DeployableDelta(reference));
		dd.registerChange(event);
	}

	private void fireState() {
		// Feels strange to allow this class to fire the event
		// but whatever. This feels so dirty. 
		if( delegate != null && delegate.getServer() != null 
				&& delegate.getServer().getServerManagementModel() != null 
				&& delegate.getServer().getServerManagementModel().getServerModel() != null ) {
			delegate.getServer().getServerManagementModel().getServerModel()
				.fireServerStateChanged(delegate.getServer(), delegate.getServerState());
		}
	}

	public void updateServerPublishStateFromDeployments() {
		updateServerPublishStateFromDeployments(false);
	}
	
	public void updateServerPublishStateFromDeployments(boolean fireEvent) {
		List<DeployableState> vals = new ArrayList<>(getStates().values());
		int newState = ServerManagementAPIConstants.PUBLISH_STATE_NONE;

		if( deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_ADD, vals) || 
				deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_REMOVE, vals) ||
				deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_FULL, vals)) {
			newState = ServerManagementAPIConstants.PUBLISH_STATE_FULL;
		} else {
			if( deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN, vals)) {
				newState = ServerManagementAPIConstants.PUBLISH_STATE_UNKNOWN;
			} else if( deployableExists(ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL, vals)) {
				newState = ServerManagementAPIConstants.PUBLISH_STATE_INCREMENTAL;
			} else {
				newState = ServerManagementAPIConstants.PUBLISH_STATE_NONE;
			}
		}
		setServerPublishState(newState, fireEvent);
	}
	
	private boolean deployableExists(int publishState, List<DeployableState> deployableStates) {
		return deployableStates.stream()
				.anyMatch(deployState -> deployState.getPublishState() == publishState);
	}

	@Override
	public int getServerPublishState() {
		return this.publishState;
	}

	@Override
	public void setServerPublishState(int state, boolean fire) {
		if( state != this.publishState) {
			this.publishState = state;
			if( fire ) 
				fireState();
		}
	}

	@Override
	public DeployableReference fillOptionsFromCache(DeployableReference reference) {
		if (reference == null) {
			return null;
		}
		reference.setOptions(deploymentOptions.get(getKey(reference)));
		return reference;
	}

	@Override
	public IDeployableResourceDelta getDeployableResourceDelta(DeployableReference reference) {
		return deltas.get(getKey(reference));
	}

}
