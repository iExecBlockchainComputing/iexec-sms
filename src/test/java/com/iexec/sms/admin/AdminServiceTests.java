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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.sms.MemoryLogAppender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@Slf4j
class AdminServiceTests {

    private final AdminService adminService;

    @TempDir
    File tempStorageLocation;

    public AdminServiceTests() {
        DataSource dataSource = DataSourceBuilder.create()
                .url("jdbc:sqlite::memory:")
                .username("sa")
                .password("sa")
                .build();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        this.adminService = new AdminService(jdbcTemplate, "jdbc:sqlite::memory:", "sa", "", "/tmp/");
    }

    @BeforeEach
    void init(TestInfo testInfo) {
        log.info(">>> {}", testInfo.getDisplayName());
    }

    private static MemoryLogAppender memoryLogAppender;

    @BeforeAll
    static void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.iexec.sms.admin");
        memoryLogAppender = new MemoryLogAppender();
        memoryLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryLogAppender);
        memoryLogAppender.start();
    }

    @BeforeEach
    void beforeEach() {
        memoryLogAppender.reset();
    }

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

    // region restore-backup
    @Test
    void shouldRestoreBackup() {
        final String backupFile = Path.of(tempStorageLocation.getPath(), "backup.sql").toString();
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertTrue(new File(backupFile).exists());
        adminService.restoreDatabaseFromBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertTrue(memoryLogAppender.contains("Backup has been restored"));
    }

    @Test
    void shouldFailToRestoreWhenBackupFileMissing() throws IOException {
        final String backupStorageLocation = tempStorageLocation.getCanonicalPath();
        assertFalse(adminService.restoreDatabaseFromBackupFile(backupStorageLocation, "backup.sql"));
    }

    @Test
    void shouldFailToRestoreWhenBackupFileOutOfStorage() {
        assertAll(
                () -> assertFalse(adminService.restoreDatabaseFromBackupFile("/backup", "backup.sql")),
                () -> assertTrue(memoryLogAppender.contains("Backup file is outside of storage file system"))
        );
    }

    @Test
    void withSQLException() {
        final String backupFile = Path.of(tempStorageLocation.getPath(), "backup.sql").toString();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(DataSourceBuilder.create().build());
        AdminService corruptAdminService = new AdminService(jdbcTemplate, "url", "username", "password", "/tmp/");
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertTrue(new File(backupFile).exists());
        corruptAdminService.restoreDatabaseFromBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertTrue(memoryLogAppender.contains("SQL error occurred during restore"));
    }
    // endregion

    // region delete-backup
    @Test
    void shouldDeleteBackup() throws IOException {
        final String backupFileName = "backup.sql";
        final Path tmpFile = Files.createFile(tempStorageLocation.toPath().resolve(backupFileName));
        assertAll(
                () -> assertTrue(adminService.deleteBackupFileFromStorage(tempStorageLocation.getPath(), backupFileName)),
                () -> assertFalse(tmpFile.toFile().exists()),
                () -> assertTrue(memoryLogAppender.contains("Successfully deleted backup"))
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
    void shouldFailToDeleteWhenBackupFileOutOfStorage() {
        assertAll(
                () -> assertFalse(adminService.deleteBackupFileFromStorage("/backup", "backup.sql")),
                () -> assertTrue(memoryLogAppender.contains("Backup file is outside of storage file system"))
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

    // region copy-backup
    @Test
    void shouldCopy() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createDatabaseBackupFile(validStorageLocation, validBackupFileName);
        assertTrue(adminService.copyBackupFile(validStorageLocation, validBackupFileName, validStorageLocation, "backup-copy.sql"));
        assertTrue(new File(validStorageLocation + File.separator + "backup-copy.sql").exists());
    }

    @Test
    void shouldFailToCopyWhenDestinationFileAlreadyExist() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertFalse(adminService.copyBackupFile(validStorageLocation, validBackupFileName, validStorageLocation, validBackupFileName));
        assertTrue(memoryLogAppender.contains(AdminService.ERR_FILE_ALREADY_EXIST));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/opt"})
    void shouldFailToCopyWhenSourceIsOutsideStorage(String location) {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertFalse(adminService.copyBackupFile(location, validBackupFileName, validStorageLocation, validBackupFileName));
        assertTrue(memoryLogAppender.contains(AdminService.ERR_BACKUP_FILE_OUTSIDE_STORAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/opt"})
    void shouldFailToCopyWhenDestinationIsOutsideStorage(String location) {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertFalse(adminService.copyBackupFile(validStorageLocation, validBackupFileName, location, ""));
        assertTrue(memoryLogAppender.contains(AdminService.ERR_REPLICATE_OR_COPY_FILE_OUTSIDE_STORAGE));
    }

    @Test
    void shouldFailToCopyWhenBackupFileDoesNotExist() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertThrows(
                FileSystemNotFoundException.class,
                () -> adminService.copyBackupFile(validStorageLocation, "backup2.sql", "", ""),
                AdminService.ERR_BACKUP_FILE_NOT_EXIST
        );
    }

    @Test
    void shouldFailToCopyWhenDestinationStorageDoesNotExist() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createDatabaseBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertFalse(adminService.copyBackupFile(validStorageLocation, validBackupFileName, "/tmp/nonexistent", validBackupFileName));
        assertTrue(memoryLogAppender.contains("An error occurred while copying backup"));
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
