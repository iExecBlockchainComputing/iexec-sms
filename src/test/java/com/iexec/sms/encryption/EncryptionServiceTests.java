/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.encryption;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.sms.MemoryLogAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTests {

    @TempDir
    public File tempDir;

    @Mock
    private EncryptionConfiguration encryptionConfiguration;

    @InjectMocks
    private EncryptionService encryptionService;

    private static MemoryLogAppender memoryLogAppender;

    @BeforeAll
    static void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.iexec.sms.encryption");
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

    @Test
    void shouldCreateAesKey() {
        String data = "data mock";
        String aesKeyPath = tempDir.getAbsolutePath() + "aes";
        EncryptionConfiguration encryptionConfiguration = new EncryptionConfiguration(aesKeyPath);
        final EncryptionService service = new EncryptionService(encryptionConfiguration);

        File aesKeyFile = new File(aesKeyPath);
        assertAll(
                () -> assertTrue(aesKeyFile.exists()),
                () -> assertTrue(aesKeyFile.canRead()),
                () -> assertFalse(aesKeyFile.canExecute()),
                () -> assertFalse(aesKeyFile.canWrite()),
                () -> assertThat(service.decrypt(service.encrypt(data))).isEqualTo(data),
                () -> assertTrue(memoryLogAppender.contains("AES key file set to readOnly")),
                () -> assertTrue(memoryLogAppender.contains("success:true"))
        );
        //second call, no need to set permissions agin
        new EncryptionService(encryptionConfiguration);
        assertAll(
                () -> assertFalse(memoryLogAppender.doesNotContains("AES key file set to readOnly")),
                () -> assertTrue(memoryLogAppender.contains("isNewAesKey:false"))
        );
    }

    @Test
    void shouldReturnEmptyIfFailedToDecrypt() {
        String aesKeyPath = tempDir.getAbsolutePath() + " aes";
        EncryptionService service = new EncryptionService(
                new EncryptionConfiguration(aesKeyPath));

        assertAll(
                () -> assertThat(service.decrypt(service.decrypt("0x"))).isEqualTo("")
        );
    }
}
