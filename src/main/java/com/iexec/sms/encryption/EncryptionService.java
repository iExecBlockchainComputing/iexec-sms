package com.iexec.sms.encryption;


import com.iexec.common.security.CipherHelper;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.FileHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.io.File;

import static com.iexec.common.utils.FileHelper.createFileWithContent;

@Slf4j
@Service
public class EncryptionService {

    private final String DEFAULT_MESSAGE = "Hello message to test AES key integrity";
    private byte[] aesKey;

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
        byte[] encryptedData = CipherHelper.aesEncrypt(data.getBytes(), aesKey);
        if (encryptedData != null) {
            return new String(encryptedData);
        }
        return "";
    }

    public String decrypt(String encryptedData) {
        byte[] decryptedData = CipherHelper.aesDecrypt(encryptedData.getBytes(), aesKey);
        if (decryptedData != null) {
            return new String(decryptedData);
        }
        return "";
    }

}
