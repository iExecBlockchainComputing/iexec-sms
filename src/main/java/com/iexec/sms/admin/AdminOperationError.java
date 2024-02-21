/*
 * Copyright 2024-2024 IEXEC BLOCKCHAIN TECH
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

/**
 * Enum to manage error on administration operations
 */
public enum AdminOperationError {

    BACKUP_FILE_OUTSIDE_STORAGE("Backup file is outside of storage file system"),
    DATABASE_BACKUP_FILE_NOT_EXIST("Database backup file does not exist"),
    AES_KEY_BACKUP_FILE_NOT_EXIST("AES KEY backup file does not exist"),
    REPLICATE_OR_COPY_FILE_OUTSIDE_STORAGE("Replicated or Copied backup file destination is outside of storage file system"),
    DATABASE_FILE_ALREADY_EXIST("A file already exists at the destination of database file"),
    AES_KEY_FILE_ALREADY_EXIST("A file already exists at the destination of AES Key file"),
    AES_KEY_FILE_WRITE_PERMISSIONS("Can't add write permissions to AES Key file permissions");

    private final String description;

    AdminOperationError(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
