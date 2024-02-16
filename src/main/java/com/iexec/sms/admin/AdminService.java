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
                    AdminOperationError.BACKUP_FILE_OUTSIDE_STORAGE);
            if (!Path.of(backupFileLocation).toFile().exists()) {
                throw new FileSystemNotFoundException(AdminOperationError.DATABASE_BACKUP_FILE_NOT_EXIST.toString());
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
                    AdminOperationError.BACKUP_FILE_OUTSIDE_STORAGE);
            final Path backupFileLocationPath = Path.of(backupFileLocation);
            if (!backupFileLocationPath.toFile().exists()) {
                throw new FileSystemNotFoundException(AdminOperationError.DATABASE_BACKUP_FILE_NOT_EXIST.toString());
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
            final Path sourceDatabaseBackupFileLocation = Path.of(checkBackupFileLocation(
                    sourceStorageLocation + File.separator + sourceBackupFileName,
                    AdminOperationError.BACKUP_FILE_OUTSIDE_STORAGE));

            // authorizations are controlled via the previous line, no need to call checkBackupFileLocation here
            final Path sourceAesKeyBackupFileLocation = Path.of(sourceDatabaseBackupFileLocation + AES_KEY_FILENAME_EXTENSION);

            // Check source backup (dump and aes key) exist
            checkSourceFileExists(sourceDatabaseBackupFileLocation, AdminOperationError.DATABASE_BACKUP_FILE_NOT_EXIST);
            checkSourceFileExists(sourceAesKeyBackupFileLocation, AdminOperationError.AES_KEY_BACKUP_FILE_NOT_EXIST);

            // Check that we want to copy into authorized location
            final Path destinationDatabaseBackupFileLocation = Path.of(checkBackupFileLocation(
                    destinationStorageLocation + File.separator + destinationBackupFileName,
                    AdminOperationError.REPLICATE_OR_COPY_FILE_OUTSIDE_STORAGE));

            // AES Key copy destination
            final Path destinationAesKeyBackupFileLocation = Path.of(destinationDatabaseBackupFileLocation + AES_KEY_FILENAME_EXTENSION);

            // Check destination
            checkDestinationFileNotExists(destinationDatabaseBackupFileLocation, AdminOperationError.DATABASE_FILE_ALREADY_EXIST);
            checkDestinationFileNotExists(destinationAesKeyBackupFileLocation, AdminOperationError.AES_KEY_FILE_ALREADY_EXIST);

            //Process copy
            processCopyFile(sourceDatabaseBackupFileLocation, destinationDatabaseBackupFileLocation, "Database");
            processCopyFile(sourceAesKeyBackupFileLocation, destinationAesKeyBackupFileLocation, "AES Key");
            return true;
        } catch (IOException e) {
            log.error("An error occurred while copying backup", e);
        }
        return false;
    }

    /**
     * Checks if the source exists and throws an exception if it does not
     *
     * @param source              The source to check
     * @param adminOperationError The custom error message
     * @throws FileSystemNotFoundException If the source does not exist
     */
    private void checkSourceFileExists(Path source, AdminOperationError adminOperationError) throws FileSystemNotFoundException {
        if (!source.toFile().exists()) {
            throw new FileSystemNotFoundException(adminOperationError.toString());
        }
    }

    /**
     * Checks if the destination does not exist and throws an exception if it does
     *
     * @param destination         The destination to check
     * @param adminOperationError The custom error message
     * @throws IOException If the destination already exist
     */
    private void checkDestinationFileNotExists(Path destination, AdminOperationError adminOperationError) throws IOException {
        if (destination.toFile().exists()) {
            throw new IOException(adminOperationError.toString());
        }
    }

    /**
     * Copy a file to another location with detailed trace information
     *
     * @param source          The current location
     * @param destination     The destination location
     * @param fileDescription Short description associated with the file for better traceability
     * @throws IOException If the copy fails
     */
    private void processCopyFile(Path source, Path destination, String fileDescription) throws IOException {
        final long size = source.toFile().length();
        log.info("{} copy process start [source:{}, destination :{}]", fileDescription, source, destination);
        final long start = System.currentTimeMillis();
        Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
        final long stop = System.currentTimeMillis();
        log.info("{} copy process done [source:{}, destination :{}, timestamp:{}, duration:{} ms, size:{}]", fileDescription, source, destination, dateFormat.format(start), stop - start, size);
    }

    String checkBackupFileLocation(String fullBackupFileName, AdminOperationError adminOperationError) throws IOException {
        final File backupFile = new File(fullBackupFileName);
        final String backupFileLocation = backupFile.getCanonicalPath();
        // Ensure that storageLocation correspond to an authorised area
        if (!backupFileLocation.startsWith(adminStorageLocation)) {
            throw new IOException(adminOperationError.toString());
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
