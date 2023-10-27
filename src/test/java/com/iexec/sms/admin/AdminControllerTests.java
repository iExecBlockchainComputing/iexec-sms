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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
class AdminControllerTests {

    private static final String STORAGE_ID = "storageID";
    private static final String FILE_NAME = "backup.sql";

    @Mock
    private ReentrantLock rLock;
    @Mock
    private AdminService adminService;
    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    // region backup
    @Test
    void shouldReturnCreatedWhenBackupSuccess() {
        Mockito.doReturn(true).when(adminService).createDatabaseBackupFile(any(), any());
        assertEquals(HttpStatus.CREATED, adminController.createBackup().getStatusCode());
    }

    @Test
    void shouldReturnErrorWhenBackupFail() {
        Mockito.doReturn(false).when(adminService).createDatabaseBackupFile(any(), any());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.createBackup().getStatusCode());
    }

    @Test
    void shouldReturnTooManyRequestWhenBackupProcessIsAlreadyRunning() throws InterruptedException {
        AdminController adminControllerWithLongAction = new AdminController(new AdminService("", "", "") {
            @Override
            public boolean createDatabaseBackupFile(String storageLocation, String backupFileName) {
                try {
                    log.info("Long createDatabaseBackupFile action is running ...");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        });

        final List<ResponseEntity<String>> responses = Collections.synchronizedList(new ArrayList<>(3));

        Thread firstThread = new Thread(() -> responses.add(adminControllerWithLongAction.createBackup()));
        Thread secondThread = new Thread(() -> responses.add(adminControllerWithLongAction.createBackup()));

        firstThread.start();
        secondThread.start();
        responses.add(adminControllerWithLongAction.createBackup());

        secondThread.join();
        firstThread.join();


        long code201 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.CREATED).count();
        long code429 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS).count();

        assertEquals(1, code201);
        assertEquals(2, code429);
    }
    // endregion

    // region replicate-backup
    @Test
    void testReplicate() {
        assertEquals(HttpStatus.OK, adminController.replicateBackup(STORAGE_ID, FILE_NAME).getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("provideBadRequestParameters")
    void testBadRequestOnReplicate(String storageID, String fileName) {
        assertEquals(HttpStatus.BAD_REQUEST, adminController.replicateBackup(storageID, fileName).getStatusCode());
    }

    @Test
    void testInternalServerErrorOnReplicate() {
        when(adminService.replicateDatabaseBackupFile(STORAGE_ID, FILE_NAME)).thenThrow(RuntimeException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.replicateBackup(STORAGE_ID, FILE_NAME).getStatusCode());
    }

    @Test
    void testInterruptedThreadOnReplicate() throws InterruptedException {
        ReflectionTestUtils.setField(adminController, "rLock", rLock);
        when(rLock.tryLock(100, TimeUnit.MILLISECONDS)).thenThrow(InterruptedException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.replicateBackup(STORAGE_ID, FILE_NAME).getStatusCode());
    }

    @Test
    void testNotFoundOnReplicate() {
        when(adminService.replicateDatabaseBackupFile(STORAGE_ID, FILE_NAME)).thenThrow(FileSystemNotFoundException.class);
        assertEquals(HttpStatus.NOT_FOUND, adminController.replicateBackup(STORAGE_ID, FILE_NAME).getStatusCode());
    }

    @Test
    void testTooManyRequestOnReplicate() throws InterruptedException {
        AdminController adminControllerWithLongAction = new AdminController(new AdminService("", "", "") {
            @Override
            public String replicateDatabaseBackupFile(String storageId, String fileName) {
                try {
                    log.info("Long replicateDatabaseBackupFile action is running ...");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return adminService.replicateDatabaseBackupFile(storageId, fileName);
            }
        });

        final List<ResponseEntity<String>> responses = Collections.synchronizedList(new ArrayList<>(3));

        Thread firstThread = new Thread(() -> responses.add(adminControllerWithLongAction.replicateBackup(STORAGE_ID, FILE_NAME)));
        Thread secondThread = new Thread(() -> responses.add(adminControllerWithLongAction.replicateBackup(STORAGE_ID, FILE_NAME)));

        firstThread.start();
        secondThread.start();
        responses.add(adminControllerWithLongAction.replicateBackup(STORAGE_ID, FILE_NAME));

        secondThread.join();
        firstThread.join();


        long code200 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.OK).count();
        long code429 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS).count();

        assertEquals(1, code200);
        assertEquals(2, code429);
    }
    // endregion

    // region restore-backup
    @Test
    void testRestore() {
        assertEquals(HttpStatus.OK, adminController.restoreBackup(STORAGE_ID, FILE_NAME).getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("provideBadRequestParameters")
    void testBadRequestOnRestore(String storageID, String fileName) {
        assertEquals(HttpStatus.BAD_REQUEST, adminController.restoreBackup(storageID, fileName).getStatusCode());
    }

    @Test
    void testInternalServerErrorOnRestore() {
        when(adminService.restoreDatabaseFromBackupFile(STORAGE_ID, FILE_NAME)).thenThrow(RuntimeException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.restoreBackup(STORAGE_ID, FILE_NAME).getStatusCode());
    }

    @Test
    void testInterruptedThreadOnRestore() throws InterruptedException {
        ReflectionTestUtils.setField(adminController, "rLock", rLock);
        when(rLock.tryLock(100, TimeUnit.MILLISECONDS)).thenThrow(InterruptedException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.restoreBackup(STORAGE_ID, FILE_NAME).getStatusCode());
    }

    @Test
    void testNotFoundOnRestore() {
        when(adminService.restoreDatabaseFromBackupFile(STORAGE_ID, FILE_NAME)).thenThrow(FileSystemNotFoundException.class);
        assertEquals(HttpStatus.NOT_FOUND, adminController.restoreBackup(STORAGE_ID, FILE_NAME).getStatusCode());
    }

    @Test
    void testTooManyRequestOnRestore() throws InterruptedException {
        AdminController adminControllerWithLongAction = new AdminController(new AdminService("", "", "") {
            @Override
            public String restoreDatabaseFromBackupFile(String storageId, String fileName) {
                try {
                    log.info("Long restoreDatabaseFromBackupFile action is running ...");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return adminService.restoreDatabaseFromBackupFile(storageId, fileName);
            }
        });

        final List<ResponseEntity<String>> responses = Collections.synchronizedList(new ArrayList<>(3));

        Thread firstThread = new Thread(() -> responses.add(adminControllerWithLongAction.restoreBackup(STORAGE_ID, FILE_NAME)));
        Thread secondThread = new Thread(() -> responses.add(adminControllerWithLongAction.restoreBackup(STORAGE_ID, FILE_NAME)));

        firstThread.start();
        secondThread.start();
        responses.add(adminControllerWithLongAction.restoreBackup(STORAGE_ID, FILE_NAME));

        secondThread.join();
        firstThread.join();


        long code200 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.OK).count();
        long code429 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS).count();

        assertEquals(1, code200);
        assertEquals(2, code429);
    }
    // endregion

    // region getStoragePathFromID
    @Test
    void testFileSystemNotFoundExceptionOnGetStoragePathFromID() {
        String storageID = convertToHex("/void");
        assertThrowsExactly(FileSystemNotFoundException.class, () -> adminController.getStoragePathFromID(storageID));
    }

    @Test
    void testGetStoragePathFromID(@TempDir Path tempDir) {
        String storageID = convertToHex(tempDir.toString());
        assertEquals(tempDir.toString(), adminController.getStoragePathFromID(storageID));
    }

    private String convertToHex(String str) {
        char[] chars = str.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString(ch));
        }
        return hex.toString();
    }
    // endregion

    private static Stream<Arguments> provideBadRequestParameters() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of(" ", " "),
                Arguments.of(STORAGE_ID, " "),
                Arguments.of(" ", FILE_NAME)
        );
    }
}
