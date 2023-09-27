/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

 @Slf4j
class EncryptionServiceTests {

    @TempDir
    public File tempDir;

    @Test
    void shouldCreateAesKey() {
        String data = "data mock";
        // File createdFile = new File(tempDir, "aesKey");
        String aesKeyPath = tempDir.getAbsolutePath() + "aesKey";

        EncryptionService service = new EncryptionService(new EncryptionConfiguration(aesKeyPath));

        assertThat(service.decrypt(service.encrypt(data))).isEqualTo(data);
    }

    @Test
    void encryptDecrypt() {
        String aesKeyPath = tempDir.getAbsolutePath() + "aesKey";

        EncryptionService service = new EncryptionService(new EncryptionConfiguration(aesKeyPath));

        for (int i = 0; i < 10 ; i++) {
            String data = RandomStringUtils.randomAlphanumeric(1, 4096);
            String encryptedData = service.encrypt(data);
            String decryptedData = service.decrypt(encryptedData);
            assertThat(decryptedData).isEqualTo(data);
        }

    }
}
