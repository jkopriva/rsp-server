/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.rsp.itests.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.jboss.tools.rsp.launching.LaunchingCore;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author jrichter
 */
public class RSPServerHandler {

    private static final int WAIT_SERVER_STARTED = 3000;
    private static final String DISTRIBUTION_FILENAME = System.getProperty("rsp-distribution.filename") + ".zip";
    private static final String DISTRIBUTION_PATH = "../distribution/target/";
    private static final String SERVER_ROOT = DISTRIBUTION_PATH + "/rsp-distribution";
    private static final File SERVER_DATA = LaunchingCore.getDataLocation();
    private static final String DATA_BACKUP = SERVER_DATA + ".backup";

    private static Process serverProcess;

    public static void prepareServer() throws ZipException {
    	ZipFile zipFile = new ZipFile(new File(DISTRIBUTION_PATH, DISTRIBUTION_FILENAME));
        zipFile.extractAll(DISTRIBUTION_PATH);
    }

    public static void startServer() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("java", "-jar", "bin/felix.jar");
        builder.directory(new File(SERVER_ROOT));
        serverProcess = builder.start();
        Thread.sleep(WAIT_SERVER_STARTED);
    }

    public static void stopServer() {
    	if (serverProcess != null) {
    		serverProcess.destroy();
    	}
    }

    public static void clearServerData(boolean backup) throws IOException {
        File dataFolder = SERVER_DATA.getAbsoluteFile();
        if (dataFolder.exists()) {
            if (backup) {
                dataFolder.renameTo(new File(DATA_BACKUP));
            } else {
                Files.walk(dataFolder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                dataFolder.delete();
            }
        }
    }

    public static void restoreBackupData(boolean force) throws IOException {        
        File backupFolder = new File(DATA_BACKUP);
        if (backupFolder.exists()) {
            if (force && SERVER_DATA.exists()) {
                clearServerData(false);
            }
            backupFolder.renameTo(SERVER_DATA);
        }
    }
}
