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

import org.h2.tools.Backup;
import org.h2.tools.Restore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;

@RestController
@RequestMapping("/backup")
public class BackupController {
    private final DataSource dataSource;

    public BackupController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<byte[]> backup() throws SQLException, IOException {
        try (Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute("SHUTDOWN");
        }
        Backup.execute("/data/backup.zip", "/data", "sms-h2", false);
        final byte[] backupContent = Files.readAllBytes(Path.of("/data/backup.zip"));
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=backup.zip")
                .body(backupContent);
    }

    @PostMapping
    public ResponseEntity<Void> restore(@RequestBody byte[] backupContent) throws IOException {
        Files.write(Path.of("/data/restore.zip"), backupContent);
        Restore.execute("/data/restore.zip", "/data", "sms-h2");
        return ResponseEntity.ok().build();
    }
}
