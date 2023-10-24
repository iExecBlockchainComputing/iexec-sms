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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class AdminControllerTests {

    @Mock
    private AdminService adminService;
    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void testBackupInAdminController() {
        assertEquals(HttpStatus.OK, adminController.createBackup().getStatusCode());
    }

    @Test
    void testRestoreInAdminController() {
        assertEquals(HttpStatus.OK, adminController.restoreBackup("", "").getStatusCode());
    }

    @Test
    void testSemaphoreBackupInAdminController() {
        AdminController adminControllerForSemaphore = new AdminController(new AdminService() {
            @Override
            public String createDatabaseBackupFile() {
                try {
                    log.info("Long createDatabaseBackupFile action is running ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return adminService.createDatabaseBackupFile();
            }
        });

        final List<ResponseEntity<String>> firstResponse = new ArrayList<>();
        ResponseEntity<String> secondResponse;
        Thread thread = new Thread(() -> {
            firstResponse.add(adminControllerForSemaphore.createBackup());
        });
        thread.start();

        secondResponse = adminControllerForSemaphore.createBackup();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, firstResponse.get(0).getStatusCode());
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
    }

    @Test
    void testSemaphoreInRestoreAdminController() {
        AdminController adminControllerForSemaphore = new AdminController(new AdminService() {
            @Override
            public String restoreDatabaseFromBackupFile(String storageId, String fileName) {
                try {
                    log.info("Long restoreDatabaseFromBackupFile action is running ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return adminService.restoreDatabaseFromBackupFile("", "");
            }
        });

        final List<ResponseEntity<String>> firstResponse = new ArrayList<>();
        ResponseEntity<String> secondResponse;
        Thread thread = new Thread(() -> {
            firstResponse.add(adminControllerForSemaphore.restoreBackup("", ""));
        });
        thread.start();

        secondResponse = adminControllerForSemaphore.restoreBackup("", "");

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, firstResponse.get(0).getStatusCode());
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
    }
}
