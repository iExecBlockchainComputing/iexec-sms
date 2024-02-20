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
import lombok.Getter;
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

    private static final String AES_KEY_LOG_DESCRIPTION = "AES Key";
    // Used to print formatted date in log
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static final String AES_KEY_FILENAME_EXTENSION = ".key";
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String adminStorageLocation;
    private final EncryptionService encryptionService;

    @Getter
    private boolean smsOnline;

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
            putSmsOffline();
            final String backupFileLocation = checkBackupFileLocation(
                    storageLocation + File.separator + backupFileName,
                    AdminOperationError.BACKUP_FILE_OUTSIDE_STORAGE);
            final Path backupDatabaseFileLocation = Path.of(backupFileLocation);

            if (!backupDatabaseFileLocation.toFile().exists()) {
                throw new FileSystemNotFoundException(AdminOperationError.DATABASE_BACKUP_FILE_NOT_EXIST.toString());
            }

            final Path backupAesKeyFileLocationPath = Path.of(backupFileLocation + AES_KEY_FILENAME_EXTENSION);
            if (!backupAesKeyFileLocationPath.toFile().exists()) {
                throw new FileSystemNotFoundException(AdminOperationError.AES_KEY_BACKUP_FILE_NOT_EXIST.toString());
            }

            final long startRestoration = System.currentTimeMillis();
            log.info("Starting the full restore process [backupFileLocation:{},backupAesKeyFileLocationPath:{}]", backupFileLocation, backupAesKeyFileLocationPath);
            restoreAesKey(backupAesKeyFileLocationPath);
            restoreDatabase(backupDatabaseFileLocation);
            final long stopRestoration = System.currentTimeMillis();
            log.info("Ending the full restore process [backupFileLocation:{},backupAesKeyFileLocationPath:{},timestamp:{}, duration:{} ms]", backupFileLocation, backupAesKeyFileLocationPath, dateFormat.format(new Date(startRestoration)), stopRestoration - startRestoration);

            return true;
        } catch (IOException e) {
            log.error("Invalid backup file operation", e);
        } catch (SQLException e) {
            log.error("SQL error occurred during restore", e);
        } finally {
            putSmsOnline();
        }
        return false;
    }

    /**
     * Restore the AES Key from backup
     *
     * @param backupAesKeyFileLocation The location of AES key backup
     * @throws IOException If an error occurred during AES Key file manipulation
     */
    private void restoreAesKey(Path backupAesKeyFileLocation) throws IOException {
        final long startAesKeyRestoration = System.currentTimeMillis();
        final long databaseAesKeyBackupFileSize = backupAesKeyFileLocation.toFile().length();
        final boolean successWrite = encryptionService.setWritePermissions();
        if (!successWrite) {
            throw new IOException(AdminOperationError.AES_KEY_FILE_WRITE_PERMISSIONS.toString());
        }
        processCopyFile(backupAesKeyFileLocation, Path.of(encryptionService.getAesKeyPath()), AES_KEY_LOG_DESCRIPTION, StandardCopyOption.REPLACE_EXISTING);
        log.info("Reload AES Key file [aesKeyFileLocationPath:{}]", encryptionService.getAesKeyPath());
        encryptionService.reloadAESKey();
        final long stopAesKeyRestoration = System.currentTimeMillis();
        log.info("AES Key file has been restored [backupAesKeyFileLocationPath:{}, timestamp:{}, duration:{} ms, size:{}]",
                backupAesKeyFileLocation, dateFormat.format(new Date(startAesKeyRestoration)), stopAesKeyRestoration - startAesKeyRestoration, databaseAesKeyBackupFileSize);
    }

    /**
     * Restore the database from backup
     *
     * @param backupDatabaseFileLocation The location of database backup
     * @throws SQLException If an error occurred during sql script execution
     */
    private void restoreDatabase(Path backupDatabaseFileLocation) throws SQLException {
        final long databaseBackupFileSize = backupDatabaseFileLocation.toFile().length();
        final long startDatabaseRestoration = System.currentTimeMillis();
        log.info("Starting the restore process for the database");
        RunScript.execute(datasourceUrl, datasourceUsername, datasourcePassword,
                backupDatabaseFileLocation.toString(), Charset.defaultCharset(), true);
        final long stopDatabaseRestoration = System.currentTimeMillis();
        log.info("Database has been restored [backupFileLocation:{}, timestamp:{}, duration:{} ms, size:{}]",
                backupDatabaseFileLocation, dateFormat.format(new Date(startDatabaseRestoration)), stopDatabaseRestoration - startDatabaseRestoration, databaseBackupFileSize);
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
            final Path backupDatabaseFileLocationPath = Path.of(backupFileLocation);
            final Path backupAesKeyFileLocationPath = Path.of(backupDatabaseFileLocationPath + AES_KEY_FILENAME_EXTENSION);

            final boolean deleteSuccessfulDB = processDeleteFile(backupDatabaseFileLocationPath, "Database");
            final boolean deleteSuccessfulAESKey = processDeleteFile(backupAesKeyFileLocationPath, AES_KEY_LOG_DESCRIPTION);

            return deleteSuccessfulDB && deleteSuccessfulAESKey;
        } catch (IOException e) {
            log.error("An error occurred while deleting backup", e);
        }
        return false;
    }

    /**
     * Delete a file if exist with detailed trace information
     *
     * @param source          The file to delete
     * @param fileDescription Short description associated with the file for better traceability
     * @return {@code true} if the deletion was successful, {@code false} if the file doesn't exist
     * @throws IOException If the deletion fails with an error
     */
    private boolean processDeleteFile(Path source, String fileDescription) throws IOException {
        //We don't want to raise an exception if one of the files doesn't exist,
        // as this could be due to an error during copy/replication or a previous delete that wasn't completely successful, so we want to have a workaround.
        if (source.toFile().exists()) {
            log.info("{} delete process start [source:{}]", fileDescription, source);
            final long start = System.currentTimeMillis();
            Files.delete(source);
            final long stop = System.currentTimeMillis();
            log.info("{} delete process done [source:{}, timestamp:{}, duration:{} ms]", fileDescription, source, dateFormat.format(start), stop - start);
            return true;
        } else {
            log.warn("{} delete process not possible, the file is not present [source:{}]", fileDescription, source);
            return false;
        }
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
            processCopyFile(sourceDatabaseBackupFileLocation, destinationDatabaseBackupFileLocation, "Database", StandardCopyOption.COPY_ATTRIBUTES);
            processCopyFile(sourceAesKeyBackupFileLocation, destinationAesKeyBackupFileLocation, AES_KEY_LOG_DESCRIPTION, StandardCopyOption.COPY_ATTRIBUTES);
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
     * @param source             The current location
     * @param destination        The destination location
     * @param fileDescription    Short description associated with the file for better traceability
     * @param standardCopyOption Copy option
     * @throws IOException If the copy fails
     */
    private void processCopyFile(Path source, Path destination, String fileDescription, StandardCopyOption standardCopyOption) throws IOException {
        final long size = source.toFile().length();
        log.info("{} copy process start [source:{}, destination :{}]", fileDescription, source, destination);
        final long start = System.currentTimeMillis();
        Files.copy(source, destination, standardCopyOption);
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

    /**
     * Put SMS offline, especially in the case of a restoration
     * where you don't want new insertion requests to arrive
     */
    void putSmsOffline() {
        log.info("SMS is now offline");
        smsOnline = false;
    }

    /**
     * Put SMS online
     */
    void putSmsOnline() {
        log.info("SMS is now online");
        smsOnline = true;
    }

}
