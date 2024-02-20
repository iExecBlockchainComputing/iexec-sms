/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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


import com.iexec.common.security.CipherHelper;
import com.iexec.common.utils.FileHelper;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import javax.annotation.PostConstruct;
import java.io.File;

import static com.iexec.common.utils.FileHelper.createFileWithContent;

@Slf4j
@Service
public class EncryptionService {

    private final String DEFAULT_MESSAGE = "Hello message to test AES key integrity";
    private byte[] aesKey;

    @Getter
    private final String aesKeyPath;

    public EncryptionService(EncryptionConfiguration configuration) {
        this.aesKeyPath = configuration.getAesKeyPath();
        this.aesKey = getOrCreateAesKey(configuration.getAesKeyPath());
    }

    @PostConstruct
    protected void checkAlgoAndPermissions() {
        if (!decrypt(encrypt(DEFAULT_MESSAGE)).equals(DEFAULT_MESSAGE)) {
            throw new ExceptionInInitializerError("AES key is corrupted");
        }
        if (!checkOrFixReadOnlyPermissions(aesKeyPath)) {
            throw new ExceptionInInitializerError("Failed to set ReadOnly permission on AES key");
        }
    }

    /**
     * Enables to reload the AES Key at runtime
     * Use after the restoration process
     */
    public void reloadAESKey() {
        log.info("Reload AES Key [aesKeyPath={}]", this.aesKeyPath);
        this.aesKey = getOrCreateAesKey(this.aesKeyPath);
        checkAlgoAndPermissions();
    }

    private byte[] getOrCreateAesKey(String aesKeyPath) {
        if (aesKeyPath == null || aesKeyPath.isEmpty()) {
            throw new ExceptionInInitializerError("Failed to get aesKeyPath");
        }

        boolean shouldGenerateKey = !new File(aesKeyPath).exists();

        if (shouldGenerateKey) {
            byte[] newAesKey = CipherHelper.generateAesKey();

            if (newAesKey == null) {
                throw new ExceptionInInitializerError("Failed to generate AES key");
            }
            if (createFileWithContent(aesKeyPath, newAesKey) == null) {
                throw new ExceptionInInitializerError("Failed to write generated AES key");
            }
        }

        byte[] aesKey = FileHelper.readFileBytes(aesKeyPath);

        if (aesKey == null) {
            throw new ExceptionInInitializerError("Failed to load AES key");
        }

        log.info("AES key loaded [isNewAesKey:{}, aesKeyPath:{}, aesKeyHash:{}]",
                shouldGenerateKey, aesKeyPath, BytesUtils.bytesToString(Hash.sha3(aesKey)));

        return aesKey;
    }

    public String encrypt(String data) {
        if (StringUtils.isNotBlank(data)) {
            byte[] encryptedData = CipherHelper.aesEncrypt(data.getBytes(), aesKey);
            if (encryptedData != null) {
                return new String(encryptedData);
            }
        }
        return "";
    }

    public String decrypt(String encryptedData) {
        if (StringUtils.isNotBlank(encryptedData)) {
            byte[] decryptedData = CipherHelper.aesDecrypt(encryptedData.getBytes(), aesKey);
            if (decryptedData != null) {
                return new String(decryptedData);
            }
        }
        return "";
    }

    boolean checkOrFixReadOnlyPermissions(String aesKeyPath) {
        final File file = new File(aesKeyPath);
        if (file.canWrite()) {
            try {
                final boolean success = file.setReadOnly();
                log.debug("AES key file set to readOnly [aesKeyPath:{}, success:{}]", aesKeyPath, success);
                return success;
            } catch (SecurityException e) {
                log.error("Unable to set AES key file to read-only [aesKeyPath:{}]", aesKeyPath, e);
                return false;
            }
        }
        return true;
    }

    /**
     * Enables the file to be written
     * Use for restoration process
     *
     * @return {@code true} if the switch was successful, {@code false} if any error occurs.
     */
    public boolean setWritePermissions() {
        log.info("Change file permissions to authorize writing");
        try {
            final File file = new File(aesKeyPath);
            final boolean success = file.setWritable(true, true);
            log.debug("AES key file set to write [aesKeyPath:{}, success:{}]", aesKeyPath, success);
            return success;
        } catch (SecurityException e) {
            log.error("Unable to set AES key file to write [aesKeyPath:{}]", aesKeyPath, e);
        }
        return false;
    }
}
