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


import com.iexec.common.security.CipherUtils;
import com.iexec.common.utils.FileHelper;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.io.File;
import java.util.Base64;

import static com.iexec.common.utils.FileHelper.createFileWithContent;

@Slf4j
@Service
public class EncryptionService {

    private final String DEFAULT_MESSAGE = "Hello message to test AES key integrity";
    private final byte[] aesKey;

    public EncryptionService(EncryptionConfiguration configuration) {
        this.aesKey = getOrCreateAesKey(configuration.getAesKeyPath());

        if (!decrypt(encrypt(DEFAULT_MESSAGE)).equals(DEFAULT_MESSAGE)) {
            throw new ExceptionInInitializerError("AES key is corrupted");
        }
    }

    private byte[] getOrCreateAesKey(String aesKeyPath) {
        if (aesKeyPath == null || aesKeyPath.isEmpty()) {
            throw new ExceptionInInitializerError("Failed to get aesKeyPath");
        }

        boolean shouldGenerateKey = !new File(aesKeyPath).exists();

        if (shouldGenerateKey) {
            byte[] newAesKey = Base64.getEncoder().encode(CipherUtils.generateAesKey());

            if (newAesKey == null) {
                throw new ExceptionInInitializerError("Failed to generate AES key");
            }
            if (createFileWithContent(aesKeyPath, newAesKey) == null) {
                throw new ExceptionInInitializerError("Failed to write generated AES key");
            }
        }

        byte[] parsedKey = FileHelper.readFileBytes(aesKeyPath);

        if (parsedKey == null) {
            throw new ExceptionInInitializerError("Failed to load AES key");
        }

        log.info("AES key loaded [isNewAesKey:{}, aesKeyPath:{}, aesKeyHash:{}]",
                shouldGenerateKey, aesKeyPath, BytesUtils.bytesToString(Hash.sha3(parsedKey)));

        return parsedKey;
    }

    public String encrypt(String data) {
        try {
            log.info("encrypting data {}", data);
            String encryptedData = Base64.getEncoder().encodeToString(CipherUtils.aesEncrypt(data.getBytes(), aesKey));
            log.info("encrypted data {}", encryptedData);
            return encryptedData;
        } catch(Exception e) {
            log.warn("Operation failed", e);
            return "";
        }
    }

    public String decrypt(String encryptedData) {
        try {
            return new String(CipherUtils.aesDecrypt(Base64.getDecoder().decode(encryptedData), aesKey));
        } catch (Exception e) {
            log.warn("Operation failed", e);
            return "";
        }
    }

}
