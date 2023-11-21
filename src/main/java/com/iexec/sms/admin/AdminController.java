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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    /**
     * Enum representing different types of backup operations: BACKUP, COPY,DELETE, REPLICATE and RESTORE.
     */
    private enum BackupAction {
        BACKUP, COPY, DELETE, REPLICATE, RESTORE;
    }

    /**
     * The directory where the database backup file will be stored.
     * The value of this constant should be a valid, existing directory path.
     */
    private static final String BACKUP_STORAGE_LOCATION = "/work/";

    /**
     * The name of the database backup file.
     */
    private static final String BACKUP_FILENAME = "backup.sql";

    /**
     * We want to perform one operation at a time. This ReentrantLock is used to set up the lock mechanism.
     */
    private final ReentrantLock rLock = new ReentrantLock(true);

    private final AdminService adminService;
    private final String adminStorageLocation;

    public AdminController(AdminService adminService, @Value("${admin.storage-location}") String adminStorageLocation) {
        this.adminService = adminService;
        this.adminStorageLocation = adminStorageLocation;
    }

    /**
     * Endpoint to initiate a database backup.
     * <p>
     * This method allows the client to trigger a database backup operation.
     * The backup process will create a snapshot of the current database
     * and store it for future recovery purposes.
     *
     * @return A response entity indicating the status and details of the backup operation.
     * <ul>
     * <li>HTTP 201 (Created) - If the backup has been successfully created.
     * <li>HTTP 429 (Too Many Requests) - If another operation (backup/restore/delete) is already in progress.
     * <li>HTTP 500 (Internal Server Error) - If an unexpected error occurs during the backup process.
     * </ul>
     */
    @PostMapping("/backup")
    public ResponseEntity<Void> createBackup() {
        return performOperation(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, BackupAction.BACKUP);
    }

    /**
     * Endpoint to replicate a database backup.
     * <p>
     * This method allows the replication of the backup toward another storage.
     *
     * @param storageID The unique identifier for the storage location of the dump in hexadecimal.
     * @param fileName  The name of the file copied on the persistent storage.
     * @return A response entity indicating the status and details of the replication operation.
     * <ul>
     * <li>HTTP 200 (OK) - If the backup has been successfully replicated.
     * <li>HTTP 400 (Bad Request) - If {@code fileName} is missing or {@code storageID} does not match an existing directory.
     * <li>HTTP 404 (Not Found) - If the backup file specified by {@code fileName} does not exist.
     * <li>HTTP 429 (Too Many Requests) - If another operation (backup/restore/delete) is already in progress.
     * <li>HTTP 500 (Internal Server Error) - If an unexpected error occurs during the replication process.
     * </ul>
     */
    @PostMapping("/{storageID}/replicate-backup")
    ResponseEntity<Void> replicateBackup(@PathVariable String storageID, @RequestParam String fileName) {
        return performOperation(storageID, fileName, StringUtils.EMPTY, StringUtils.EMPTY, BackupAction.REPLICATE);
    }

    /**
     * Endpoint to restore a database backup.
     * <p>
     * This method allows the client to initiate the restoration of a database backup
     * from a specified dump file, identified by the {@code fileName}, located in a location specified by the {@code storageID}.
     *
     * @param storageID The unique identifier for the storage location of the dump in hexadecimal.
     * @param fileName  The name of the dump file to be restored.
     * @return A response entity indicating the status and details of the restore operation.
     * <ul>
     * <li>HTTP 200 (OK) - If the backup has been successfully restored.
     * <li>HTTP 400 (Bad Request) - If {@code fileName} is missing or {@code storageID} does not match an existing directory.
     * <li>HTTP 404 (Not Found) - If the backup file specified by {@code fileName} does not exist.
     * <li>HTTP 429 (Too Many Requests) - If another operation (backup/restore/delete) is already in progress.
     * <li>HTTP 500 (Internal Server Error) - If an unexpected error occurs during the restore process.
     * </ul>
     */
    @PostMapping("/{storageID}/restore-backup")
    ResponseEntity<Void> restoreBackup(@PathVariable String storageID, @RequestParam String fileName) {
        return performOperation(storageID, fileName, StringUtils.EMPTY, StringUtils.EMPTY, BackupAction.RESTORE);
    }

    /**
     * Endpoint to delete a database backup.
     * <p>
     * This method allows the client to initiate the deletion of a database backup
     * from a specified dump file, identified by the {@code fileName}, located in a location specified by the {@code storageID}.
     *
     * @param storageID The unique identifier for the storage location of the dump in hexadecimal.
     * @param fileName  The name of the dump file to be deleted.
     * @return A response entity indicating the status and details of the delete operation.
     * <ul>
     * <li>HTTP 200 (OK) - If the backup has been successfully deleted.
     * <li>HTTP 400 (Bad Request) - If {@code fileName} is missing or {@code storageID} does not match an existing directory.
     * <li>HTTP 404 (Not Found) - If the backup file specified by {@code fileName} does not exist.
     * <li>HTTP 429 (Too Many Requests) - If another operation (backup/restore/replicate) is already in progress.
     * <li>HTTP 500 (Internal Server Error) - If an unexpected error occurs during the restore process.
     * </ul>
     */
    @DeleteMapping("/{storageID}/delete-backup")
    ResponseEntity<Void> deleteBackup(@PathVariable String storageID, @RequestParam String fileName) {
        return performOperation(storageID, fileName, StringUtils.EMPTY, StringUtils.EMPTY, BackupAction.DELETE);
    }

    /**
     * Endpoint to copy a database backup.
     * <p>
     * This method allows the copy of the backup toward another storage.
     *
     * @param sourceStorageID      The unique identifier for the source storage location of the dump in hexadecimal.
     * @param sourceFileName       The name of the source file to copy from the source storage location.
     * @param destinationStorageID The unique identifier for the destination storage location of the dump in hexadecimal.
     * @param destinationFileName  The name of the destination file, can be empty.
     * @return A response entity indicating the status and details of the copy operation.
     * <ul>
     * <li>HTTP 200 (OK) - If the copy has been successfully replicated.
     * <li>HTTP 400 (Bad Request) - If {@code sourceFileName} is missing or {@code sourceStorageID} or {@code destinationStorageID}  does not match an existing directory.
     * <li>HTTP 404 (Not Found) - If the backup file specified by {@code fileName} does not exist.
     * <li>HTTP 429 (Too Many Requests) - If another operation (backup/restore/delete/replicate/copy) is already in progress.
     * <li>HTTP 500 (Internal Server Error) - If an unexpected error occurs during the copy process.
     * </ul>
     */
    @PostMapping("/{sourceStorageID}/copy-to/{destinationStorageID}")
    ResponseEntity<Void> copyBackup(@PathVariable String sourceStorageID, @PathVariable String destinationStorageID, @RequestParam String sourceFileName, @RequestParam(required = false) String destinationFileName) {
        return performOperation(sourceStorageID, sourceFileName, destinationStorageID, destinationFileName, BackupAction.COPY);
    }

    /**
     * Common method for database backup operations.
     *
     * @param sourceStorageID The unique identifier for the storage location of the dump in hexadecimal.
     * @param sourceFileName  The name of the dump file to be operated on.
     * @param operationType   The type of operation {{@link BackupAction}.
     * @return A response entity indicating the status and details of the operation.
     */
    private ResponseEntity<Void> performOperation(String sourceStorageID, String sourceFileName, String destinationStorageID, String destinationFileName, BackupAction operationType) {
        try {
            if ((StringUtils.isBlank(sourceStorageID) || StringUtils.isBlank(sourceFileName)) && operationType != BackupAction.BACKUP) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (StringUtils.isBlank(destinationStorageID) && operationType == BackupAction.COPY) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (!tryToAcquireLock()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }

            final String sourceStoragePath = getStoragePathFromID(sourceStorageID);

            boolean operationSuccessful = false;

            switch (operationType) {
                case BACKUP:
                    operationSuccessful = adminService.createDatabaseBackupFile(adminStorageLocation + BACKUP_STORAGE_LOCATION, BACKUP_FILENAME);
                    break;
                case RESTORE:
                    operationSuccessful = adminService.restoreDatabaseFromBackupFile(sourceStoragePath, sourceFileName);
                    break;
                case DELETE:
                    operationSuccessful = adminService.deleteBackupFileFromStorage(sourceStoragePath, sourceFileName);
                    break;
                case REPLICATE:
                    operationSuccessful = adminService.replicateDatabaseBackupFile(
                            adminStorageLocation + BACKUP_STORAGE_LOCATION, BACKUP_FILENAME, sourceStoragePath, sourceFileName);
                    break;
                case COPY:
                    final String destinationStoragePath = getStoragePathFromID(destinationStorageID);
                    operationSuccessful = adminService.copyBackupFileFromStorageToStorage(
                            sourceStoragePath, sourceFileName, destinationStoragePath, destinationFileName);
                    break;
                default:
                    break;
            }

            if (operationSuccessful) {
                if (operationType == BackupAction.BACKUP) {
                    return ResponseEntity.status(HttpStatus.CREATED).build();
                }
                return ResponseEntity.ok().build();
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (FileSystemNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            tryToReleaseLock();
        }
    }

    /**
     * Converts {@code storageID} to an ascii string and checks if it is an existing folder.
     *
     * @param storageID The hexadecimal representation of an ascii String
     * @return The storage path if it exists, throws a {@code FileSystemNotFoundException} otherwise.
     */
    String getStoragePathFromID(String storageID) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < storageID.length(); i += 2) {
            String str = storageID.substring(i, i + 2);
            sb.append((char) Integer.parseInt(str, 16));
        }
        final String output = sb.toString();
        if (!Files.isDirectory(Path.of(output))) {
            throw new FileSystemNotFoundException("Storage ID " + storageID + " does not match an existing folder");
        }
        return output;
    }

    private boolean tryToAcquireLock() throws InterruptedException {
        return rLock.tryLock(100, TimeUnit.MILLISECONDS);
    }

    private void tryToReleaseLock() {
        if (rLock.isHeldByCurrentThread()) {
            rLock.unlock();
        }
    }

}
