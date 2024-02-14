/*
 * Copyright 2023-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.sms.encryption.EncryptionService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
public class AdminService {

    // Used to print formatted date in log
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static final String AES_KEY_FILENAME_EXTENSION = ".key";
    public static final String ERR_BACKUP_FILE_OUTSIDE_STORAGE = "Backup file is outside of storage file system";
    public static final String ERR_BACKUP_FILE_NOT_EXIST = "Backup file does not exist";
    public static final String ERR_REPLICATE_OR_COPY_FILE_OUTSIDE_STORAGE = "Replicated or Copied backup file destination is outside of storage file system";
    public static final String ERR_FILE_ALREADY_EXIST = "A file already exists at the destination";
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String adminStorageLocation;
    private final EncryptionService encryptionService;

    public AdminService(EncryptionService encryptionService,
                        @Value("${spring.datasource.url}") String datasourceUrl,
                        @Value("${spring.datasource.username}") String datasourceUsername,
                        @Value("${spring.datasource.password}") String datasourcePassword,
                        @Value("${admin.storage-location}") String adminStorageLocation) {
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.adminStorageLocation = adminStorageLocation;
        this.encryptionService = encryptionService;
    }

    /**
     * Creates a backup of the H2 database and associated AES key at the specified location.
     *
     * @param storageLocation The location where the backup file will be saved, must be an existing directory.
     * @param backupFileName  The name of the backup file.
     * @return {@code true} if the backup was successful, {@code false} if any error occurs.
     */
    boolean createBackupFile(String storageLocation, String backupFileName) {
        try {
            // Ensure that storageLocation and backupFileName are not blanks
            boolean validation = checkCommonParameters(storageLocation, backupFileName);
            if (!validation) {
                return false;
            }
            // Check if storageLocation is an existing directory, we don't want to create it.
            final File directory = new File(storageLocation);
            if (!directory.isDirectory()) {
                log.error("storageLocation must be an existing directory [storageLocation:{}]", storageLocation);
                return false;
            }
            final File backupFile = new File(storageLocation + File.separator + backupFileName);
            final String databaseBackupFileLocation = backupFile.getCanonicalPath();
            final String aesKeyBackupFileLocation = databaseBackupFileLocation + AES_KEY_FILENAME_EXTENSION;
            //Backup aes key
            Files.copy(Path.of(encryptionService.getAesKeyPath()), Path.of(aesKeyBackupFileLocation), StandardCopyOption.REPLACE_EXISTING);
            log.debug("Backup AES Key created [fileName:{}]", aesKeyBackupFileLocation);
            return databaseDump(databaseBackupFileLocation);
        } catch (IOException e) {
            log.error("An error occurred while creating backup", e);
        }
        return false;
    }

    /**
     * @param fullBackupFileName complete fileName (location and filename)
     * @return {@code true} if the backup was successful, {@code false} if any error occurs.
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
            log.info("New backup created [fullBackupFileName:{}, timestamp:{}, duration:{} ms, size:{}]",
                    fullBackupFileName, dateFormat.format(new Date(start)), stop - start, size);
        } catch (SQLException e) {
            log.error("SQL error occurred during backup", e);
            return false;
        }
        return true;
    }

    /**
     * Restores a backup from provided inputs.
     * <p>
     * The location is checked against a configuration property value provided by an admin.
     *
     * @param storageLocation Where to find the backup file
     * @param backupFileName  The file to restore
     * @return {@code true} if the restoration was successful, {@code false} if any error occurs.
     */
    boolean restoreDatabaseFromBackupFile(String storageLocation, String backupFileName) {
        try {
            final String backupFileLocation = checkBackupFileLocation(
                    storageLocation + File.separator + backupFileName,
                    ERR_BACKUP_FILE_OUTSIDE_STORAGE);
            if (!Path.of(backupFileLocation).toFile().exists()) {
                throw new FileSystemNotFoundException(ERR_BACKUP_FILE_NOT_EXIST);
            }
            final long size = backupFileLocation.length();
            log.info("Starting the restore process [backupFileLocation:{}]", backupFileLocation);
            final long start = System.currentTimeMillis();
            RunScript.execute(datasourceUrl, datasourceUsername, datasourcePassword,
                    backupFileLocation, Charset.defaultCharset(), true);
            final long stop = System.currentTimeMillis();
            log.warn("Backup has been restored [backupFileLocation:{}, timestamp:{}, duration:{} ms, size:{}]",
                    backupFileLocation, dateFormat.format(new Date(start)), stop - start, size);
            return true;
        } catch (IOException e) {
            log.error("Invalid backup file location", e);
        } catch (SQLException e) {
            log.error("SQL error occurred during restore", e);
        }
        return false;
    }

    /**
     * Delete a backup of the H2 database from a location
     *
     * @param storageLocation The location of the backup file.
     * @param backupFileName  The name of the backup file.
     * @return {@code true} if the deletion was successful, {@code false} if any error occurs.
     */
    boolean deleteBackupFileFromStorage(String storageLocation, String backupFileName) {
        try {
            // Ensure that storageLocation and backupFileName are not blanks
            boolean validation = checkCommonParameters(storageLocation, backupFileName);
            if (!validation) {
                return false;
            }
            final String backupFileLocation = checkBackupFileLocation(
                    storageLocation + File.separator + backupFileName,
                    ERR_BACKUP_FILE_OUTSIDE_STORAGE);
            final Path backupFileLocationPath = Path.of(backupFileLocation);
            if (!backupFileLocationPath.toFile().exists()) {
                throw new FileSystemNotFoundException(ERR_BACKUP_FILE_NOT_EXIST);
            }
            log.info("Starting the delete process [backupFileLocation:{}]", backupFileLocation);
            final long start = System.currentTimeMillis();
            Files.delete(backupFileLocationPath);
            final long stop = System.currentTimeMillis();
            log.info("Successfully deleted backup [backupFileLocation:{}, timestamp:{}, duration:{} ms]",
                    backupFileLocation, dateFormat.format(new Date(start)), stop - start);
            return true;
        } catch (IOException e) {
            log.error("An error occurred while deleting backup", e);
        }
        return false;
    }

    /**
     * Copy a backup from a location to another with the possibility of renaming the file
     * <p>
     * The {@code sourceStorageLocation/sourceBackupFileName} is replicated to {@code destinationStorageLocation/destinationBackupFileName}.
     *
     * @param sourceStorageLocation      The location of the source backup file.
     * @param sourceBackupFileName       The name of the source backup file.
     * @param destinationStorageLocation The location of destination the backup file.
     * @param destinationBackupFileName  The name of the destination backup file.
     * @return {@code true} if the copy was successful, {@code false} if any error occurs.
     */
    boolean copyBackupFile(String sourceStorageLocation, String sourceBackupFileName, String destinationStorageLocation, String destinationBackupFileName) {
        try {
            // Check that we want to copy an authorised file
            final Path sourceBackupFileLocation = Path.of(checkBackupFileLocation(
                    sourceStorageLocation + File.separator + sourceBackupFileName,
                    ERR_BACKUP_FILE_OUTSIDE_STORAGE));

            // File must exist
            if (!sourceBackupFileLocation.toFile().exists()) {
                throw new FileSystemNotFoundException(ERR_BACKUP_FILE_NOT_EXIST);
            }

            // Check that we want to copy into authorized location
            final Path destinationBackupFileLocation = Path.of(checkBackupFileLocation(
                    destinationStorageLocation + File.separator + destinationBackupFileName,
                    ERR_REPLICATE_OR_COPY_FILE_OUTSIDE_STORAGE));

            // Check that we are not trying to overwrite a file
            if (destinationBackupFileLocation.toFile().exists()) {
                throw new IOException(ERR_FILE_ALREADY_EXIST);
            }
            final long size = sourceBackupFileLocation.toFile().length();
            log.info("Starting the copy process [sourceBackupFileLocation:{}, destinationBackupFileLocation:{}]", sourceBackupFileLocation, destinationBackupFileLocation);
            final long start = System.currentTimeMillis();
            Files.copy(sourceBackupFileLocation, destinationBackupFileLocation, StandardCopyOption.COPY_ATTRIBUTES);
            final long stop = System.currentTimeMillis();
            log.info("Backup has been copied [sourceBackupFileLocation:{}, destinationBackupFileLocation:{}, timestamp:{}, duration:{} ms, size:{}]",
                    sourceBackupFileLocation, destinationBackupFileLocation, dateFormat.format(start), stop - start, size);
            return true;
        } catch (IOException e) {
            log.error("An error occurred while copying backup", e);
        }
        return false;
    }

    String checkBackupFileLocation(String fullBackupFileName, String errorMessage) throws IOException {
        final File backupFile = new File(fullBackupFileName);
        final String backupFileLocation = backupFile.getCanonicalPath();
        // Ensure that storageLocation correspond to an authorised area
        if (!backupFileLocation.startsWith(adminStorageLocation)) {
            throw new IOException(errorMessage);
        }
        return backupFileLocation;
    }


    boolean checkCommonParameters(String storageLocation, String backupFileName) {
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
        return true;
    }

}
