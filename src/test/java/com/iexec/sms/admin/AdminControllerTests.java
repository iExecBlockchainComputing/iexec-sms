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

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final String STORAGE_PATH = "/storage";
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
        AdminController adminControllerWithLongAction = new AdminController(new AdminService("", "", "", "") {
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
        }, "");

        final List<ResponseEntity<Void>> responses = Collections.synchronizedList(new ArrayList<>(3));

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
    void testReplicate(@TempDir Path tempDir) {
        final String storageID = convertToHex(tempDir.toString());
        ReflectionTestUtils.setField(adminController, "adminStorageLocation", tempDir.toString());
        when(adminService.copyBackupFile(tempDir + "/work/", FILE_NAME, tempDir.toString(), FILE_NAME)).thenReturn(true);
        assertEquals(HttpStatus.OK, adminController.replicateBackup(storageID, FILE_NAME).getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("provideBadRequestParameters")
    void testBadRequestOnReplicate(String storageID, String fileName) {
        assertEquals(HttpStatus.BAD_REQUEST, adminController.replicateBackup(storageID, fileName).getStatusCode());
    }

    @Test
    void testInternalServerErrorOnReplicate() {
        when(adminService.copyBackupFile(STORAGE_PATH, FILE_NAME, STORAGE_PATH, FILE_NAME)).thenThrow(RuntimeException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.replicateBackup(STORAGE_PATH, FILE_NAME).getStatusCode());
    }

    @Test
    void testInterruptedThreadOnReplicate() throws InterruptedException {
        ReflectionTestUtils.setField(adminController, "rLock", rLock);
        when(rLock.tryLock(100, TimeUnit.MILLISECONDS)).thenThrow(InterruptedException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.replicateBackup(STORAGE_PATH, FILE_NAME).getStatusCode());
    }

    @Test
    void testNotFoundOnReplicate() {
        final String storageID = convertToHex(STORAGE_PATH);
        when(adminService.copyBackupFile(STORAGE_PATH, FILE_NAME, STORAGE_PATH, FILE_NAME)).thenThrow(FileSystemNotFoundException.class);
        assertEquals(HttpStatus.NOT_FOUND, adminController.replicateBackup(storageID, FILE_NAME).getStatusCode());
    }

    @Test
    void testTooManyRequestOnReplicate(@TempDir Path tempDir) throws InterruptedException {
        AdminController adminControllerWithLongAction = new AdminController(new AdminService("", "", "", "") {
            @Override
            public boolean copyBackupFile(String backupStoragePath, String backupFileName, String replicateStoragePath, String replicateFileName) {
                try {
                    log.info("Long copyOrReplicateBackupFileFromStorageToStorage action is running ...");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        }, "");

        final List<ResponseEntity<Void>> responses = Collections.synchronizedList(new ArrayList<>(3));
        final String storageID = convertToHex(tempDir.toString());

        Thread firstThread = new Thread(() -> responses.add(adminControllerWithLongAction.replicateBackup(storageID, FILE_NAME)));
        Thread secondThread = new Thread(() -> responses.add(adminControllerWithLongAction.replicateBackup(storageID, FILE_NAME)));

        firstThread.start();
        secondThread.start();
        responses.add(adminControllerWithLongAction.replicateBackup(storageID, FILE_NAME));

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
    void testRestore(@TempDir Path tempDir) {
        final String storageID = convertToHex(tempDir.toString());
        when(adminService.restoreDatabaseFromBackupFile(tempDir.toString(), FILE_NAME)).thenReturn(true);
        assertEquals(HttpStatus.OK, adminController.restoreBackup(storageID, FILE_NAME).getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("provideBadRequestParameters")
    void testBadRequestOnRestore(String storageID, String fileName) {
        assertEquals(HttpStatus.BAD_REQUEST, adminController.restoreBackup(storageID, fileName).getStatusCode());
    }

    @Test
    void testInternalServerErrorOnRestore(@TempDir Path tempDir) {
        final String storageID = convertToHex(tempDir.toString());
        when(adminService.restoreDatabaseFromBackupFile(tempDir.toString(), FILE_NAME)).thenThrow(RuntimeException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.restoreBackup(storageID, FILE_NAME).getStatusCode());
    }

    @Test
    void testInterruptedThreadOnRestore() throws InterruptedException {
        ReflectionTestUtils.setField(adminController, "rLock", rLock);
        when(rLock.tryLock(100, TimeUnit.MILLISECONDS)).thenThrow(InterruptedException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.restoreBackup(STORAGE_PATH, FILE_NAME).getStatusCode());
    }

    @Test
    void testNotFoundOnRestore() {
        final String storageID = convertToHex(STORAGE_PATH);
        when(adminService.restoreDatabaseFromBackupFile(STORAGE_PATH, FILE_NAME)).thenThrow(FileSystemNotFoundException.class);
        assertEquals(HttpStatus.NOT_FOUND, adminController.restoreBackup(storageID, FILE_NAME).getStatusCode());
    }

    @Test
    void testTooManyRequestOnRestore(@TempDir Path tempDir) throws InterruptedException {
        AdminController adminControllerWithLongAction = new AdminController(new AdminService("", "", "", "") {
            @Override
            public boolean restoreDatabaseFromBackupFile(String storageId, String fileName) {
                try {
                    log.info("Long restoreDatabaseFromBackupFile action is running ...");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        }, "");

        final List<ResponseEntity<Void>> responses = Collections.synchronizedList(new ArrayList<>(3));
        final String storageID = convertToHex(tempDir.toString());

        Thread firstThread = new Thread(() -> responses.add(adminControllerWithLongAction.restoreBackup(storageID, FILE_NAME)));
        Thread secondThread = new Thread(() -> responses.add(adminControllerWithLongAction.restoreBackup(storageID, FILE_NAME)));

        firstThread.start();
        secondThread.start();
        responses.add(adminControllerWithLongAction.restoreBackup(storageID, FILE_NAME));

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

    private static String convertToHex(String str) {
        char[] chars = str.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString(ch));
        }
        return hex.toString();
    }
    // endregion

    // region delete-backup
    @Test
    void testDelete(@TempDir Path tempDir) {
        final String storageID = convertToHex(tempDir.toString());
        when(adminService.deleteBackupFileFromStorage(tempDir.toString(), FILE_NAME)).thenReturn(true);
        assertEquals(HttpStatus.OK, adminController.deleteBackup(storageID, FILE_NAME).getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("provideBadRequestParameters")
    void testBadRequestOnDelete(String storageID, String fileName) {
        assertEquals(HttpStatus.BAD_REQUEST, adminController.deleteBackup(storageID, fileName).getStatusCode());
    }

    @Test
    void testInternalServerErrorOnDelete(@TempDir Path tempDir) {
        final String storageID = convertToHex(tempDir.toString());
        when(adminService.deleteBackupFileFromStorage(tempDir.toString(), FILE_NAME)).thenThrow(RuntimeException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.deleteBackup(storageID, FILE_NAME).getStatusCode());
    }

    @Test
    void testInterruptedThreadOnDelete() throws InterruptedException {
        ReflectionTestUtils.setField(adminController, "rLock", rLock);
        when(rLock.tryLock(100, TimeUnit.MILLISECONDS)).thenThrow(InterruptedException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.deleteBackup(STORAGE_PATH, FILE_NAME).getStatusCode());
    }

    @Test
    void testNotFoundOnDelete() {
        final String storageID = convertToHex(STORAGE_PATH);
        when(adminService.deleteBackupFileFromStorage(STORAGE_PATH, FILE_NAME)).thenThrow(FileSystemNotFoundException.class);
        assertEquals(HttpStatus.NOT_FOUND, adminController.deleteBackup(storageID, FILE_NAME).getStatusCode());
    }

    @Test
    void testTooManyRequestOnDelete(@TempDir Path tempDir) throws InterruptedException {
        AdminController adminControllerWithLongAction = new AdminController(new AdminService("", "", "", "") {
            @Override
            public boolean deleteBackupFileFromStorage(String storageLocation, String backupFileName) {
                try {
                    log.info("Long restoreDatabaseFromBackupFile action is running ...");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        }, "");

        final List<ResponseEntity<Void>> responses = Collections.synchronizedList(new ArrayList<>(3));
        final String storageID = convertToHex(tempDir.toString());

        Thread firstThread = new Thread(() -> responses.add(adminControllerWithLongAction.deleteBackup(storageID, FILE_NAME)));
        Thread secondThread = new Thread(() -> responses.add(adminControllerWithLongAction.deleteBackup(storageID, FILE_NAME)));

        firstThread.start();
        secondThread.start();
        responses.add(adminControllerWithLongAction.deleteBackup(storageID, FILE_NAME));

        secondThread.join();
        firstThread.join();


        long code200 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.OK).count();
        long code429 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS).count();

        assertEquals(1, code200);
        assertEquals(2, code429);
    }
    // endregion

    // region replicate-backup
    @Test
    void testCopy(@TempDir Path tempDir) {
        final String sourceStorageID = convertToHex(tempDir.toString());

        ReflectionTestUtils.setField(adminController, "adminStorageLocation", tempDir.toString());
        when(adminService.copyBackupFile(tempDir.toString(), FILE_NAME, tempDir.toString(), "backup2.sql")).thenReturn(true);
        assertEquals(HttpStatus.OK, adminController.copyBackup(sourceStorageID, sourceStorageID, FILE_NAME, "backup2.sql").getStatusCode());
    }

    @Test
    void testCopyWithTheSameName(@TempDir Path tempDir) throws IOException {
        final String sourceStorageID = convertToHex(tempDir.toString());
        final Path dailyDir = Paths.get(tempDir.toString(), "daily");
        final String dailyDirString = Files.createDirectories(dailyDir).toFile().getAbsolutePath();
        final String destinationStorageID = convertToHex(dailyDirString);

        ReflectionTestUtils.setField(adminController, "adminStorageLocation", tempDir.toString());
        when(adminService.copyBackupFile(tempDir.toString(), FILE_NAME, dailyDirString, FILE_NAME)).thenReturn(true);
        assertEquals(HttpStatus.OK, adminController.copyBackup(sourceStorageID, destinationStorageID, FILE_NAME, "").getStatusCode());
    }

    @Test
    void testInterruptedThreadOnCopy() throws InterruptedException {
        ReflectionTestUtils.setField(adminController, "rLock", rLock);
        when(rLock.tryLock(100, TimeUnit.MILLISECONDS)).thenThrow(InterruptedException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.copyBackup(STORAGE_PATH, STORAGE_PATH, FILE_NAME, FILE_NAME).getStatusCode());
    }

    @Test
    void testInternalServerErrorOnCopy(@TempDir Path tempDir) {
        final String sourceStorageID = convertToHex(tempDir.toString());
        when(adminService.copyBackupFile(tempDir.toString(), FILE_NAME, tempDir.toString(), "backup2.sql")).thenThrow(RuntimeException.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, adminController.copyBackup(sourceStorageID, sourceStorageID, FILE_NAME, "backup2.sql").getStatusCode());
    }

    @Test
    void testTooManyRequestOnCopy(@TempDir Path tempDir) throws InterruptedException {

        AdminController adminControllerWithLongAction = new AdminController(new AdminService("", "", "", "") {
            @Override
            public boolean copyBackupFile(String sourceStorageLocation, String sourceBackupFileName, String destinationStorageLocation, String destinationBackupFileName) {
                try {
                    log.info("Long copyOrReplicateBackupFileFromStorageToStorage action is running ...");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        }, "");

        final List<ResponseEntity<Void>> responses = Collections.synchronizedList(new ArrayList<>(3));
        final String sourceStorageID = convertToHex(tempDir.toString());

        Thread firstThread = new Thread(() -> responses.add(adminControllerWithLongAction.copyBackup(sourceStorageID, sourceStorageID, FILE_NAME, "backup2.sql")));
        Thread secondThread = new Thread(() -> responses.add(adminControllerWithLongAction.copyBackup(sourceStorageID, sourceStorageID, FILE_NAME, "backup2.sql")));

        firstThread.start();
        secondThread.start();
        responses.add(adminControllerWithLongAction.copyBackup(sourceStorageID, sourceStorageID, FILE_NAME, "backup2.sql"));

        secondThread.join();
        firstThread.join();


        long code200 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.OK).count();
        long code429 = responses.stream().filter(element -> element.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS).count();

        assertEquals(1, code200);
        assertEquals(2, code429);
    }

    @ParameterizedTest
    @MethodSource("provideBadRequestParameters")
    void testBadRequestOnCopyOnCommonsAdminControls(String storageID, String fileName) {
        assertEquals(HttpStatus.BAD_REQUEST, adminController.copyBackup(storageID, "NOT_CONCERNED", fileName, "NOT_CONCERNED").getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("provideBadRequestParametersForCopy")
    void testBadRequestOnCopyOnSpecificsCopyControls(String sourceStorageID, String sourceFileName, String destinationStorageID) {
        assertEquals(HttpStatus.BAD_REQUEST, adminController.copyBackup(sourceStorageID, destinationStorageID, sourceFileName, "NOT_CONCERNED").getStatusCode());
    }
    // endregion

    private static Stream<Arguments> provideBadRequestParameters() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of(" ", " "),
                Arguments.of(STORAGE_PATH, " "),
                Arguments.of(" ", FILE_NAME)
        );
    }

    private static Stream<Arguments> provideBadRequestParametersForCopy() {
        return Stream.of(
                Arguments.of(STORAGE_PATH, FILE_NAME, ""),
                Arguments.of(STORAGE_PATH, FILE_NAME, " "),
                Arguments.of(STORAGE_PATH, FILE_NAME, null)
        );
    }
}
