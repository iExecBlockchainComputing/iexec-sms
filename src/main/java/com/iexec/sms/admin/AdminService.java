/*
 * Copyright 2023 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iexec.sms.admin;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.h2.tools.RunScript;
import org.h2.tools.Script;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
public class AdminService {

    // Used to print formatted date in log
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String adminStorageLocation;

    public AdminService(@Value("${spring.datasource.url}") String datasourceUrl,
                        @Value("${spring.datasource.username}") String datasourceUsername,
                        @Value("${spring.datasource.password}") String datasourcePassword,
                        @Value("${admin.storage-location}") String adminStorageLocation) {
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.adminStorageLocation = adminStorageLocation;
    }

    /**
     * Creates a backup of the H2 database and saves it to the specified location.
     *
     * @param storageLocation The location where the backup file will be saved, must be an existing directory.
     * @param backupFileName  The name of the backup file.
     * @return {@code true} if the backup was successful; {@code false} if any error occurs.
     */
    boolean createDatabaseBackupFile(String storageLocation, String backupFileName) {
        // Check for missing or empty storageLocation parameter
        if (StringUtils.isBlank(storageLocation)) {
            log.error("storageLocation must not be empty.");
            return false;
        }
        // Check for missing or empty backupFileName parameter
        if (StringUtils.isBlank(backupFileName)) {
            log.error("backupFileName must not be empty.");
            return false;
        }
        // Ensure that storageLocation ends with a slash
        if (!storageLocation.endsWith(File.separator)) {
            storageLocation += File.separator;
        }

        // Check if storageLocation is an existing directory, we don't want to create it.
        final File directory = new File(storageLocation);
        if (!directory.isDirectory()) {
            log.error("storageLocation must be an existing directory [storageLocation:{}]", storageLocation);
            return false;
        }

        return databaseDump(storageLocation + backupFileName);
    }

    /**
     * @param fullBackupFileName complete fileName (location and filename)
     * @return {@code true} if the backup was successful; {@code false} if any error occurs.
     */
    boolean databaseDump(String fullBackupFileName) {

        if (StringUtils.isBlank(fullBackupFileName)) {
            log.error("fullBackupFileName must not be empty.");
            return false;
        }

        try {
            log.info("Starting the backup process [fullBackupFileName:{}]", fullBackupFileName);
            final long start = System.currentTimeMillis();
            Script.process(datasourceUrl, datasourceUsername, datasourcePassword, fullBackupFileName, "DROP", "");
            final long stop = System.currentTimeMillis();
            final long size = new File(fullBackupFileName).length();
            log.info("New backup created [timestamp:{}, duration:{} ms, size:{}, fullBackupFileName:{}]", dateFormat.format(new Date(start)), stop - start, size, fullBackupFileName);
        } catch (SQLException e) {
            log.error("SQL error occurred during backup", e);
            return false;
        }
        return true;
    }

    public String replicateDatabaseBackupFile(String storagePath, String backupFileName) {
        return "replicateDatabaseBackupFile is not implemented";
    }

    /**
     * Restores a backup from provided inputs.
     * <p>
     * The location is checked against a configuration property value provided by an admin.
     *
     * @param storageLocation Where to find the backup file
     * @param backupFileName  The file to restore
     * @return {@code true} if the restoration was successful, {@code false} otherwise.
     */
    boolean restoreDatabaseFromBackupFile(String storageLocation, String backupFileName) {
        try {
            // checks on backup file to restore
            final File backupFile = new File(storageLocation + File.separator + backupFileName);
            final String backupFileLocation = backupFile.getCanonicalPath();
            if (!backupFileLocation.startsWith(adminStorageLocation)) {
                throw new IOException("Backup file is outside of storage file system");
            } else if (!backupFile.exists()) {
                throw new FileSystemNotFoundException("Backup file does not exist");
            }
            final long size = backupFileLocation.length();
            final long start = System.currentTimeMillis();
            RunScript.execute(datasourceUrl, datasourceUsername, datasourcePassword,
                    backupFileLocation, Charset.defaultCharset(), true);
            final long stop = System.currentTimeMillis();
            log.warn("Backup has been restored [timestamp:{}, duration:{} ms, size:{}, fullBackupFileName:{}]",
                    dateFormat.format(new Date(start)), stop - start, size, backupFileLocation);
            return true;
        } catch (IOException e) {
            log.error("Invalid backup file location", e);
        } catch (SQLException e) {
            log.error("SQL error occurred during restore", e);
        }
        return false;
    }
}
