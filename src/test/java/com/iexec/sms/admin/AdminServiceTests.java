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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(OutputCaptureExtension.class)
class AdminServiceTests {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:test")
                .username("sa")
                .password("")
                .build();
    }

    private final AdminService adminService = new AdminService("jdbc:h2:mem:test", "sa", "", "/tmp/");
    @TempDir
    File tempStorageLocation;

    // region backup
    @Test
    void shouldReturnTrueWhenAllParametersAreValid() {
        assertTrue(adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql"));
    }


    @Test
    void shouldReturnFalseWhenAllParametersAreValidButBackupFailed() {
        AdminService adminServiceSpy = Mockito.spy(adminService);

        Mockito.doReturn(false).when(adminServiceSpy).databaseDump(any());
        assertFalse(adminServiceSpy.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldReturnFalseWhenEmptyOrNullStorageLocation(String location) {
        assertFalse(adminService.createDatabaseBackupFile(location, "backup.sql"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldReturnFalseWhenEmptyOrNullBackupFileName(String fileName) {
        assertFalse(adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), fileName));
    }

    @Test
    void shouldReturnFalseWhenStorageLocationDoesNotExist() {
        assertFalse(adminService.createDatabaseBackupFile("/nonexistent/directory/", "backup.sql"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldReturnFalseWhenEmptyOrNullFullBackupFileName(String fullBackupFileName) {
        assertFalse(adminService.databaseDump(fullBackupFileName));
    }

    @Test
    void shouldReturnFalseWhenBackupFileNameDoesNotExist() {
        assertFalse(adminService.databaseDump("/nonexistent/directory/backup.sql"));
    }

    // endregion

    // region replicate-backup
    @Test
    void shouldReturnNotImplementedWhenCallingReplicate() {
        assertEquals("replicateDatabaseBackupFile is not implemented", adminService.replicateDatabaseBackupFile("", ""));
    }
    // endregion

    // region restore-backup
    @Test
    void shouldReturnNotImplementedWhenCallingRestore() {
        assertEquals("restoreDatabaseFromBackupFile is not implemented", adminService.restoreDatabaseFromBackupFile("", ""));
    }
    // endregion

    // region delete-backup
    @Test
    void shouldDeleteBackup(CapturedOutput output) throws IOException {
        final String backupFileName = "backup.sql";
        final Path tmpFile = Files.createFile(tempStorageLocation.toPath().resolve(backupFileName));
        assertAll(
                () -> assertTrue(adminService.deleteBackupFileFromStorage(tempStorageLocation.getPath(), backupFileName)),
                () -> assertFalse(tmpFile.toFile().exists()),
                () -> assertTrue(output.getOut().contains("Successfully deleted backup"))
        );
    }

    @Test
    void shouldFailedDeleteWithBackupFileMissing(CapturedOutput output) {
        final String backupFileName = "backup.sql";
        assertAll(
                () -> assertFalse(adminService.deleteBackupFileFromStorage(tempStorageLocation.getPath(), backupFileName)),
                () -> assertFalse(adminService.deleteBackupFileFromStorage(tempStorageLocation.getPath(), "")),
                () -> assertFalse(adminService.deleteBackupFileFromStorage("", backupFileName)),
                () -> assertTrue(output.getOut().contains("Backup file does not exist"))
        );
    }

    @Test
    void shouldFailedToDeleteWithBackupFileOutOfStorage(CapturedOutput output) {
        assertAll(
                () -> assertFalse(adminService.deleteBackupFileFromStorage("/backup", "backup.sql")),
                () -> assertTrue(output.getOut().contains("Backup file is outside of storage file system"))
        );
    }

    // endregion

    //region utils

    @Test
    void testIsPathInBaseDirectory() {
        assertAll(
                () -> assertFalse(adminService.isPathInBaseDirectory("/tmp/../../backup.sql")),
                () -> assertTrue(adminService.isPathInBaseDirectory("/tmp/backup.sql")),
                () -> assertTrue(adminService.isPathInBaseDirectory("/tmp/backup-copy.sql"))
        );
    }

    @Test
    void testNormalizePathWithSeparator() {
        String path1 = "test/path/";
        String path2 = "test/path";
        assertAll(
                () -> assertEquals(path1, adminService.normalizePathWithSeparator(path1)),
                () -> assertEquals(path1, adminService.normalizePathWithSeparator(path2))
        );
    }

    @Test
    void testCommonsParametersValidation() {
        // Valid case
        final String validStorageLocation = "test/path/";
        final String validBackupFileName = "backup.sql";
        final String emptyStorageLocation = "";
        final String emptyBackupFileName = "";

        assertAll(
                () -> assertTrue(adminService.commonsParametersValidation(validStorageLocation, validBackupFileName)),
                () -> assertFalse(adminService.commonsParametersValidation(emptyStorageLocation, validBackupFileName)),
                () -> assertFalse(adminService.commonsParametersValidation(validStorageLocation, emptyBackupFileName)),
                () -> assertFalse(adminService.commonsParametersValidation(emptyStorageLocation, emptyBackupFileName))
        );
    }
    // endregion
}