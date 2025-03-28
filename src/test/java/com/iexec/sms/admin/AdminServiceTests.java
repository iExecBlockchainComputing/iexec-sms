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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.sms.MemoryLogAppender;
import com.iexec.sms.encryption.EncryptionConfiguration;
import com.iexec.sms.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;

class AdminServiceTests {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:test")
                .username("sa")
                .password("")
                .build();
    }

    private AdminService adminService;

    @TempDir
    File tempStorageLocation;

    @TempDir
    public File tempDir;
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
        final EncryptionService encryptionService = new EncryptionService(
                new EncryptionConfiguration(tempDir.getAbsolutePath() + "/aes.key"));
        adminService = new AdminService(encryptionService, "jdbc:h2:mem:test", "sa", "", "/tmp/");
    }

    // region backup
    @Test
    void shouldReturnTrueWhenAllParametersAreValid() {
        final File backupAesKeyFile = new File(tempStorageLocation.getPath() + "/backup.sql" + AdminService.AES_KEY_FILENAME_EXTENSION);
        assertAll(
                () -> assertThat(adminService.createBackupFile(tempStorageLocation.getPath(), "backup.sql")).isTrue(),
                () -> assertThat(backupAesKeyFile).exists(),
                () -> assertThat(memoryLogAppender.contains("Backup AES Key created")).isTrue()
        );
    }

    @Test
    void shouldReturnFalseWhenAllParametersAreValidButBackupFailed() {
        AdminService adminServiceSpy = Mockito.spy(adminService);

        Mockito.doReturn(false).when(adminServiceSpy).databaseDump(any());
        assertThat(adminServiceSpy.createBackupFile(tempStorageLocation.getPath(), "backup.sql")).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldReturnFalseWhenEmptyOrNullStorageLocation(String location) {
        assertThat(adminService.createBackupFile(location, "backup.sql")).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldReturnFalseWhenEmptyOrNullBackupFileName(String fileName) {
        assertThat(adminService.createBackupFile(tempStorageLocation.getPath(), fileName)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenStorageLocationDoesNotExist() {
        assertThat(adminService.createBackupFile("/nonexistent/directory/", "backup.sql")).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldReturnFalseWhenEmptyOrNullFullBackupFileName(String fullBackupFileName) {
        assertThat(adminService.databaseDump(fullBackupFileName)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenBackupFileNameDoesNotExist() {
        assertThat(adminService.databaseDump("/nonexistent/directory/backup.sql")).isFalse();
    }
    // endregion

    // region restore-backup
    @Test
    void shouldRestoreBackup() {
        final String backupFile = Path.of(tempStorageLocation.getPath(), "backup.sql").toString();
        adminService.createBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertThat(new File(backupFile)).exists();
        adminService.restoreDatabaseFromBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertAll(
                () -> assertThat(memoryLogAppender.contains("AES Key file has been restored")).isTrue(),
                () -> assertThat(memoryLogAppender.contains("Database has been restored")).isTrue(),
                () -> assertThat(memoryLogAppender.contains("SMS is now online")).isTrue()
        );
    }

    @Test
    void shouldFailToRestoreWhenBackupFileMissing() throws IOException {
        final String backupStorageLocation = tempStorageLocation.getCanonicalPath();
        assertAll(
                () -> assertThatExceptionOfType(FileSystemNotFoundException.class)
                        .isThrownBy(
                                () -> adminService.restoreDatabaseFromBackupFile(backupStorageLocation, "backup.sql")
                        ).withMessageContaining(AdminOperationError.DATABASE_BACKUP_FILE_NOT_EXIST.toString()),
                () -> assertThat(memoryLogAppender.contains("SMS is now online")).isTrue()
        );
    }

    @Test
    void shouldFailToRestoreWhenAesKeyFileMissing() {
        final String backupName = "backup.sql";
        final String backupPath = tempStorageLocation.getPath();
        final File backupAesKeyFile = new File(backupPath + "/" + backupName + AdminService.AES_KEY_FILENAME_EXTENSION);
        adminService.createBackupFile(tempStorageLocation.getPath(), backupName);
        assertAll(
                () -> assertThat(backupAesKeyFile.delete()).isTrue(),
                () -> assertThatExceptionOfType(FileSystemNotFoundException.class)
                        .isThrownBy(
                                () -> adminService.restoreDatabaseFromBackupFile(backupPath, backupName)
                        ).withMessageContaining(AdminOperationError.AES_KEY_BACKUP_FILE_NOT_EXIST.toString()),
                () -> assertThat(memoryLogAppender.contains("SMS is now online")).isTrue()
        );
    }

    @Test
    void shouldFailToRestoreWhenSwitchPermissionToWriteOnAesKeyFileFail() {
        final EncryptionService encryptionServiceSpy = Mockito.spy(new EncryptionService(
                new EncryptionConfiguration(tempDir.getAbsolutePath() + "/aes.key")));
        Mockito.doReturn(false).when(encryptionServiceSpy).setWritePermissions();

        final AdminService adminServiceCorrupt = new AdminService(encryptionServiceSpy, "jdbc:h2:mem:test", "sa", "", "/tmp/");
        final String backupName = "backup.sql";
        assertAll(
                () -> assertThat(adminServiceCorrupt.createBackupFile(tempStorageLocation.getPath(), backupName)).isTrue(),
                () -> assertThat(adminServiceCorrupt.restoreDatabaseFromBackupFile(tempStorageLocation.getPath(), backupName)).isFalse(),
                () -> assertThat(memoryLogAppender.contains(AdminOperationError.AES_KEY_FILE_WRITE_PERMISSIONS.toString())).isTrue(),
                () -> assertThat(memoryLogAppender.contains("SMS is now online")).isTrue()
        );
    }

    @Test
    void shouldFailToRestoreWhenBackupFileOutOfStorage() {
        assertAll(
                () -> assertThat(adminService.restoreDatabaseFromBackupFile("/backup", "backup.sql")).isFalse(),
                () -> assertThat(memoryLogAppender.contains("Backup file is outside of storage file system")).isTrue()
        );
    }

    @Test
    void withSQLException() {
        final EncryptionService encryptionService = new EncryptionService(
                new EncryptionConfiguration(tempDir.getAbsolutePath() + "/aes.key"));
        final String backupFile = Path.of(tempStorageLocation.getPath(), "backup.sql").toString();
        final AdminService corruptAdminService = new AdminService(encryptionService, "url", "username", "password", "/tmp/");
        adminService.createBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertThat(new File(backupFile)).exists();
        corruptAdminService.restoreDatabaseFromBackupFile(tempStorageLocation.getPath(), "backup.sql");
        assertThat(memoryLogAppender.contains("SQL error occurred during restore")).isTrue();
    }
    // endregion

    // region delete-backup
    @Test
    void shouldDeleteBackup() {
        final String backupFileName = "backup.sql";
        final File backupDatabaseFile = new File(tempStorageLocation.getPath() + "/" + backupFileName);
        final File backupAesKeyFile = new File(tempStorageLocation.getPath() + "/" + backupFileName + AdminService.AES_KEY_FILENAME_EXTENSION);

        assertAll(
                () -> assertThat(adminService.createBackupFile(tempStorageLocation.getPath(), "backup.sql")).isTrue(),
                () -> assertThat(backupDatabaseFile).exists(),
                () -> assertThat(backupAesKeyFile).exists(),
                () -> assertThat(adminService.deleteBackupFileFromStorage(tempStorageLocation.getPath(), backupFileName)).isTrue(),
                () -> assertThat(backupDatabaseFile).doesNotExist(),
                () -> assertThat(backupAesKeyFile).doesNotExist(),
                () -> assertThat(memoryLogAppender.contains("Database delete process done")).isTrue(),
                () -> assertThat(memoryLogAppender.contains("AES Key delete process done")).isTrue()
        );
    }

    @Test
    void shouldFailToDeleteWhenOneBackupFileIsMissing() {
        final String backupFileName = "backup.sql";
        final File backupDatabaseFile = new File(tempStorageLocation.getPath() + "/" + backupFileName);
        final File backupAesKeyFile = new File(tempStorageLocation.getPath() + "/" + backupFileName + AdminService.AES_KEY_FILENAME_EXTENSION);

        assertAll(
                () -> assertThat(adminService.createBackupFile(tempStorageLocation.getPath(), "backup.sql")).isTrue(),
                () -> assertThat(backupDatabaseFile).exists(),
                () -> assertThat(backupAesKeyFile).exists(),
                () -> assertThat(backupAesKeyFile.delete()).isTrue(),
                () -> assertThat(adminService.deleteBackupFileFromStorage(tempStorageLocation.getCanonicalPath(), "backup.sql")).isFalse(),
                () -> assertThat(backupDatabaseFile).doesNotExist(),
                () -> assertThat(memoryLogAppender.contains("Database delete process done")).isTrue(),
                () -> assertThat(memoryLogAppender.contains("AES Key delete process not possible, the file is not present")).isTrue()
        );
    }

    @Test
    void shouldFailToDeleteWhenAllBackupFilesMissing() {
        assertAll(
                () -> assertThat(adminService.deleteBackupFileFromStorage(tempStorageLocation.getCanonicalPath(), "backup.sql")).isFalse(),
                () -> assertThat(memoryLogAppender.contains("Database delete process not possible, the file is not present")).isTrue(),
                () -> assertThat(memoryLogAppender.contains("AES Key delete process not possible, the file is not present")).isTrue()
        );
    }

    @Test
    void shouldFailToDeleteWhenBackupFileOutOfStorage() {
        assertAll(
                () -> assertThat(adminService.deleteBackupFileFromStorage("/backup", "backup.sql")).isFalse(),
                () -> assertThat(memoryLogAppender.contains("Backup file is outside of storage file system")).isTrue()
        );

    }

    @Test
    void shouldFailToDeleteWhenInvalidParameters() {
        final String validStorageLocation = "test/path/";
        final String validBackupFileName = "backup.sql";
        final String emptyStorageLocation = "";
        final String emptyBackupFileName = "";

        assertAll(
                () -> assertThat(adminService.deleteBackupFileFromStorage(emptyStorageLocation, validBackupFileName)).isFalse(),
                () -> assertThat(adminService.deleteBackupFileFromStorage(validStorageLocation, emptyBackupFileName)).isFalse(),
                () -> assertThat(adminService.deleteBackupFileFromStorage(emptyStorageLocation, emptyBackupFileName)).isFalse()
        );
    }
    // endregion

    // region copy-backup
    @Test
    void shouldCopy() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createBackupFile(validStorageLocation, validBackupFileName);
        assertAll(
                () -> assertThat(adminService.copyBackupFile(validStorageLocation, validBackupFileName, validStorageLocation, "backup-copy.sql")).isTrue(),
                () -> assertThat(new File(validStorageLocation + File.separator + "backup-copy.sql")).exists(),
                () -> assertThat(new File(validStorageLocation + File.separator + "backup-copy.sql" + AdminService.AES_KEY_FILENAME_EXTENSION)).exists()
        );
    }

    @Test
    void shouldFailToCopyWhenDatabaseDestinationFileAlreadyExist() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createBackupFile(tempStorageLocation.getPath(), validBackupFileName);
        assertAll(
                () -> assertThat(adminService.copyBackupFile(validStorageLocation, validBackupFileName, validStorageLocation, validBackupFileName)).isFalse(),
                () -> assertThat(memoryLogAppender.contains(AdminOperationError.DATABASE_FILE_ALREADY_EXIST.toString())).isTrue()
        );
    }

    @Test
    void shouldFailToCopyWhenAesKeyDestinationFileAlreadyExist() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createBackupFile(validStorageLocation, validBackupFileName);
        assertAll(
                () -> assertThat(new File(validStorageLocation + File.separator + "backup-copy.sql" + AdminService.AES_KEY_FILENAME_EXTENSION).createNewFile()).isTrue(),
                () -> assertThat(adminService.copyBackupFile(validStorageLocation, validBackupFileName, validStorageLocation, "backup-copy.sql")).isFalse(),
                () -> assertThat(memoryLogAppender.contains(AdminOperationError.AES_KEY_FILE_ALREADY_EXIST.toString())).isTrue()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/opt"})
    void shouldFailToCopyWhenSourceIsOutsideStorage(String location) {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertThat(adminService.copyBackupFile(location, validBackupFileName, validStorageLocation, validBackupFileName)).isFalse();
        assertThat(memoryLogAppender.contains(AdminOperationError.BACKUP_FILE_OUTSIDE_STORAGE.toString())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/opt"})
    void shouldFailToCopyWhenDestinationIsOutsideStorage(String location) {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertThat(adminService.copyBackupFile(validStorageLocation, validBackupFileName, location, "")).isFalse();
        assertThat(memoryLogAppender.contains(AdminOperationError.REPLICATE_OR_COPY_FILE_OUTSIDE_STORAGE.toString())).isTrue();
    }

    @Test
    void shouldFailToCopyWhenDatabaseBackupFileDoesNotExist() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createBackupFile(tempStorageLocation.getPath(), validBackupFileName);
        assertThatExceptionOfType(FileSystemNotFoundException.class)
                .isThrownBy(
                        () -> adminService.copyBackupFile(validStorageLocation, "backup2.sql", "", "")
                ).withMessageContaining(AdminOperationError.DATABASE_BACKUP_FILE_NOT_EXIST.toString());
    }

    @Test
    void shouldFailToCopyWhenAesKeyBackupFileDoesNotExist() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertAll(
                () -> assertThat(new File(tempStorageLocation.getPath() + "/" + validBackupFileName + AdminService.AES_KEY_FILENAME_EXTENSION).delete()).isTrue(),
                () -> assertThatExceptionOfType(FileSystemNotFoundException.class)
                        .isThrownBy(
                                () -> adminService.copyBackupFile(validStorageLocation, validBackupFileName, validStorageLocation, "backup-copy")
                        ).withMessageContaining(AdminOperationError.AES_KEY_BACKUP_FILE_NOT_EXIST.toString())
        );
    }

    @Test
    void shouldFailToCopyWhenDestinationStorageDoesNotExist() {
        final String validStorageLocation = tempStorageLocation.getPath();
        final String validBackupFileName = "backup.sql";
        adminService.createBackupFile(tempStorageLocation.getPath(), validBackupFileName);

        assertThat(adminService.copyBackupFile(validStorageLocation, validBackupFileName, "/tmp/nonexistent", validBackupFileName)).isFalse();
        assertThat(memoryLogAppender.contains("An error occurred while copying backup")).isTrue();
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
                () -> assertThat(adminService.checkCommonParameters(validStorageLocation, validBackupFileName)).isTrue(),
                () -> assertThat(adminService.checkCommonParameters(emptyStorageLocation, validBackupFileName)).isFalse(),
                () -> assertThat(adminService.checkCommonParameters(validStorageLocation, emptyBackupFileName)).isFalse(),
                () -> assertThat(adminService.checkCommonParameters(emptyStorageLocation, emptyBackupFileName)).isFalse()
        );
    }
    // endregion
}
