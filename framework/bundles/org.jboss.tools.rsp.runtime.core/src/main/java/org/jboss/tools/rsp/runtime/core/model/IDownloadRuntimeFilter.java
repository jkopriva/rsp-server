/*************************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.rsp.runtime.core.model;

/**
 * A filter for DownloadRuntime objects
 */
public interface IDownloadRuntimeFilter {
	/**
	 * Does this filter accept the given DownloadRuntime?
	 * @param runtime
	 * @return
	 */ 
	public boolean accepts(DownloadRuntime runtime);
	
	/**
	 * Return a filtered array from the provided array
	 * @param runtime
	 * @return
	 */ 
	public DownloadRuntime[] filter(DownloadRuntime[] runtime);
}
