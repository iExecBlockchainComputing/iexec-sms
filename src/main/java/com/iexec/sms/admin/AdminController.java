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

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
    public ResponseEntity<String> createBackup() {
        try {
            if (!tryToAcquireLock()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
            if (adminService.createDatabaseBackupFile(BACKUP_STORAGE_LOCATION, BACKUP_FILENAME)) {
                return ResponseEntity.status(HttpStatus.CREATED).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
    public ResponseEntity<String> replicateBackup(@PathVariable String storageID, @RequestParam String fileName) {
        try {
            if (StringUtils.isBlank(storageID) || StringUtils.isBlank(fileName)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (!tryToAcquireLock()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
            final String storagePath = getStoragePathFromID(storageID);
            return ResponseEntity.ok(adminService.replicateDatabaseBackupFile(storagePath, fileName));
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
        try {
            if (StringUtils.isBlank(storageID) || StringUtils.isBlank(fileName)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (!tryToAcquireLock()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
            final String storagePath = getStoragePathFromID(storageID);
            if (adminService.restoreDatabaseFromBackupFile(storagePath, fileName)) {
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
