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
                    System.out.println("Long createDatabaseBackupFile action is running ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return adminService.createDatabaseBackupFile();
            }
        });

        final List<ResponseEntity<String>> one = new ArrayList<>();
        ResponseEntity<String> two;
        Thread thread = new Thread(() -> {
            one.add(adminControllerForSemaphore.createBackup());
        });
        thread.start();

        two = adminControllerForSemaphore.createBackup();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, one.get(0).getStatusCode());
        assertEquals(HttpStatus.OK, two.getStatusCode());
    }

    @Test
    void testSemaphoreInRestoreAdminController() {
        AdminController adminControllerForSemaphore = new AdminController(new AdminService() {
            @Override
            public String restoreDatabaseFromBackupFile(String storageId, String fileName) {
                try {
                    System.out.println("Long restoreDatabaseFromBackupFile action is running ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return adminService.restoreDatabaseFromBackupFile("", "");
            }
        });

        final List<ResponseEntity<String>> one = new ArrayList<>();
        ResponseEntity<String> two;
        Thread thread = new Thread(() -> {
            one.add(adminControllerForSemaphore.restoreBackup("", ""));
        });
        thread.start();

        two = adminControllerForSemaphore.restoreBackup("", "");

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, one.get(0).getStatusCode());
        assertEquals(HttpStatus.OK, two.getStatusCode());
    }
}
