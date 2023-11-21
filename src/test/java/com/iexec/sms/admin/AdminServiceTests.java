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
import java.nio.file.FileSystemNotFoundException;
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
    void shouldReplicateBackup() {
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql");
        adminService.replicateDatabaseBackupFile(
                tempStorageLocation.getPath(), "backup.sql",
                tempStorageLocation.getPath(), "backup-copy.sql");
        assertTrue(Path.of(tempStorageLocation.getPath(), "backup-copy.sql").toFile().isFile());
    }

    @Test
    void shouldFailToReplicateWithBackupFileMissing(CapturedOutput output) {
        adminService.replicateDatabaseBackupFile(
                tempStorageLocation.getPath(), "backup.sql",
                tempStorageLocation.getPath(), "backup-copy.sql");
        assertAll(
                () -> assertFalse(Path.of(tempStorageLocation.getPath(), "backup-copy.sql").toFile().isFile()),
                () -> assertTrue(output.getOut().contains("NoSuchFileException"))
        );
    }

    @Test
    void shouldFailToReplicateWithInvalidStorageLocation(CapturedOutput output) {
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql");
        boolean replicateStatus = adminService.replicateDatabaseBackupFile(
                tempStorageLocation.getPath(), "backup.sql",
                "/tmp/nonexistent/directory", "backup-copy.sql");
        assertAll(
                () -> assertFalse(replicateStatus),
                () -> assertTrue(output.getAll().contains("NoSuchFileException"))
        );
    }

    @Test
    void shouldFailToReplicateWithOutOfStorageLocation(CapturedOutput output) {
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql");
        boolean replicateStatus = adminService.replicateDatabaseBackupFile(
                tempStorageLocation.getPath(), "backup.sql",
                "/opt", "backup-copy.sql");
        assertAll(
                () -> assertFalse(replicateStatus),
                () -> assertTrue(output.getAll().contains("Replicated backup file destination is outside of storage file system"))
        );
    }
    // endregion

    // region restore-backup
    @Test
    void shouldRestoreBackup(CapturedOutput output) {
        final String backupFile = Path.of(tempStorageLocation.getPath(), "backup.sql").toString();
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertTrue(new File(backupFile).exists());
        adminService.restoreDatabaseFromBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertTrue(output.getOut().contains("Backup has been restored"));
    }

    @Test
    void shouldFailToRestoreWhenBackupFileMissing() throws IOException {
        final String backupStorageLocation = tempStorageLocation.getCanonicalPath();
        assertThrows(
                FileSystemNotFoundException.class,
                () -> adminService.restoreDatabaseFromBackupFile(backupStorageLocation, "backup.sql"),
                "Backup file does not exist"
        );
    }

    @Test
    void shouldFailToRestoreWhenBackupFileOutOfStorage(CapturedOutput output) {
        assertAll(
                () -> assertFalse(adminService.restoreDatabaseFromBackupFile("/backup", "backup.sql")),
                () -> assertTrue(output.getOut().contains("Backup file is outside of storage file system"))
        );
    }

    @Test
    void withSQLException(CapturedOutput output) {
        final String backupFile = Path.of(tempStorageLocation.getPath(), "backup.sql").toString();
        AdminService corruptAdminService = new AdminService("url", "username", "password", "/tmp/");
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertTrue(new File(backupFile).exists());
        corruptAdminService.restoreDatabaseFromBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertTrue(output.getOut().contains("SQL error occurred during restore"));
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
    void shouldFailToDeleteWhenBackupFileMissing() throws IOException {
        final String backupStorageLocation = tempStorageLocation.getCanonicalPath();
        assertThrows(
                FileSystemNotFoundException.class,
                () -> adminService.deleteBackupFileFromStorage(backupStorageLocation, "backup.sql"),
                "Backup file does not exist"
        );
    }

    @Test
    void shouldFailToDeleteWhenBackupFileOutOfStorage(CapturedOutput output) {
        assertAll(
                () -> assertFalse(adminService.deleteBackupFileFromStorage("/backup", "backup.sql")),
                () -> assertTrue(output.getOut().contains("Backup file is outside of storage file system"))
        );
    }

    @Test
    void shouldFailToDeleteWhenInvalidParameters() {
        final String validStorageLocation = "test/path/";
        final String validBackupFileName = "backup.sql";
        final String emptyStorageLocation = "";
        final String emptyBackupFileName = "";

        assertAll(
                () -> assertFalse(adminService.deleteBackupFileFromStorage(emptyStorageLocation, validBackupFileName)),
                () -> assertFalse(adminService.deleteBackupFileFromStorage(validStorageLocation, emptyBackupFileName)),
                () -> assertFalse(adminService.deleteBackupFileFromStorage(emptyStorageLocation, emptyBackupFileName))
        );
    }
    // endregion

    //region copy-backup

    @Test
    void shouldCopy() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createDatabaseBackupFile(validStorageLocation, validBackupFileName);
        assertTrue(adminService.copyBackupFileFromStorageToStorage(validStorageLocation, validBackupFileName, validStorageLocation, "backup2.sql"));
        assertTrue(new File(validStorageLocation + File.separator + "backup2.sql").exists());
    }

    @Test
    void shouldFailToCopyWhenInvalidParameters(CapturedOutput output) {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        final String emptyStorageLocation = "";
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertAll(

                () -> assertAll(
                        () -> assertFalse(adminService.copyBackupFileFromStorageToStorage(validStorageLocation, validBackupFileName, validStorageLocation, validBackupFileName)),
                        () -> assertTrue(output.getOut().contains(AdminService.ERR_FILE_ALREADY_EXIST))
                ),

                () -> assertAll(
                        () -> assertFalse(adminService.copyBackupFileFromStorageToStorage(validStorageLocation, validBackupFileName, "", "")),
                        () -> assertTrue(output.getOut().contains(AdminService.ERR_BACKUP_FILE_OUTSIDE_STORAGE))
                ),

                () -> assertAll(
                        () -> assertFalse(adminService.copyBackupFileFromStorageToStorage(emptyStorageLocation, validBackupFileName, "", "")),
                        () -> assertTrue(output.getOut().contains(AdminService.ERR_BACKUP_FILE_OUTSIDE_STORAGE))
                ),

                () -> assertThrows(
                        FileSystemNotFoundException.class,
                        () -> adminService.copyBackupFileFromStorageToStorage(validStorageLocation, "backup2.sql", "", ""),
                        AdminService.ERR_BACKUP_FILE_NOT_EXIST
                )
        );
    }
    // endregion

    //region utils

    @Test
    void testCheckCommonParametersValidation() {
        // Valid case
        final String validStorageLocation = "test/path/";
        final String validBackupFileName = "backup.sql";
        final String emptyStorageLocation = "";
        final String emptyBackupFileName = "";

        assertAll(
                () -> assertTrue(adminService.checkCommonParameters(validStorageLocation, validBackupFileName)),
                () -> assertFalse(adminService.checkCommonParameters(emptyStorageLocation, validBackupFileName)),
                () -> assertFalse(adminService.checkCommonParameters(validStorageLocation, emptyBackupFileName)),
                () -> assertFalse(adminService.checkCommonParameters(emptyStorageLocation, emptyBackupFileName))
        );
    }
    // endregion
}
