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

import com.iexec.sms.secret.compute.TeeTaskComputeSecretRepository;
import com.iexec.sms.secret.web2.Web2SecretRepository;
import com.iexec.sms.secret.web3.Web3SecretRepository;
import lombok.extern.slf4j.Slf4j;
import org.h2.tools.RunScript;
import org.h2.tools.Script;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.sql.SQLException;

@RestController
@RequestMapping("/backup")
@Slf4j
public class BackupController {

    private final Web2SecretRepository web2SecretRepository;
    private final Web3SecretRepository web3SecretRepository;
    private final TeeTaskComputeSecretRepository teeTaskComputeSecretRepository;

    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;

    public BackupController(Web2SecretRepository web2SecretRepository,
                            Web3SecretRepository web3SecretRepository,
                            TeeTaskComputeSecretRepository teeTaskComputeSecretRepository,
                            @Value("${spring.datasource.url}")String datasourceUrl,
                            @Value("${spring.datasource.username}")String datasourceUsername,
                            @Value("${spring.datasource.password}")String datasourcePassword) {
        this.web2SecretRepository = web2SecretRepository;
        this.web3SecretRepository = web3SecretRepository;
        this.teeTaskComputeSecretRepository = teeTaskComputeSecretRepository;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
    }

    @GetMapping
    public ResponseEntity<Void> backup() throws SQLException {
        final long start = System.currentTimeMillis();
        Script.process(datasourceUrl, datasourceUsername, datasourcePassword, "/backup/backup.sql", "", "");
        final long stop = System.currentTimeMillis();
        log.info("Backup took {} ms", stop - start);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping
    public ResponseEntity<Void> restore() {
        try {
            final long start = System.currentTimeMillis();
            RunScript.execute(datasourceUrl, datasourceUsername, datasourcePassword, "/backup/backup.sql", Charset.defaultCharset(), true);
            final long stop = System.currentTimeMillis();
            log.info("Restore took {} ms", stop - start);
            return ResponseEntity.ok().build();
        } catch (SQLException e) {
            log.error("Can't restore DB", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> reset() {
        web2SecretRepository.deleteAll();
        web3SecretRepository.deleteAll();
        teeTaskComputeSecretRepository.deleteAll();

        return ResponseEntity.noContent().build();
    }
}
