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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class AdminServiceTests {


    private final AdminService adminService = new AdminService("", "", "");

    @TempDir
    File tempStorageLocation;

    // region backup
    @Test
    void shouldReturnTrueWhenAllParametersAreValid() {
        AdminService adminServiceSpy = Mockito.spy(adminService);

        Mockito.doReturn(true).when(adminServiceSpy).databaseDump(any());
        assertTrue(adminServiceSpy.createDatabaseBackupFile(tempStorageLocation.getPath(), "backup.sql"));
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
}
