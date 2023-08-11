/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.backup;

import lombok.extern.slf4j.Slf4j;
import org.h2.message.DbException;
import org.h2.tools.Backup;
import org.h2.tools.Restore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;

@RestController
@RequestMapping("/backup")
@Slf4j
public class BackupController {
    private final DataSource dataSource;

    public BackupController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Void> backup() throws SQLException {
        try (Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute("SHUTDOWN");
        }
        Backup.execute("/backup/backup.zip", "/data", "sms-h2", false);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping
    public ResponseEntity<Void> restore() {
        try {
            Restore.execute("/backup/backup.zip", "/data", "sms-h2");
            return ResponseEntity.ok().build();
        } catch (DbException e) {
            log.error("Can't restore DB", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
