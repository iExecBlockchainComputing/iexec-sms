package com.iexec.sms.iexecsms.encryption;


import com.iexec.common.security.CipherHelper;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.FileHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

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

        byte[] aesKey = FileHelper.readFileBytes(aesKeyPath);

        if (aesKey == null) {
            byte[] newAesKey = CipherHelper.generateAesKey();

            if (newAesKey == null) {
                throw new ExceptionInInitializerError("Failed to create AES key");
            }
            if (createFileWithContent(aesKeyPath, newAesKey) == null) {
                throw new ExceptionInInitializerError("Failed to write AES key");
            }

            aesKey = FileHelper.readFileBytes(aesKeyPath);
            log.info("AES key created [aesKeyPath:{}, aesKeyHash:{}]",
                    aesKeyPath, BytesUtils.bytesToString(Hash.sha3(aesKey)));
        } else {
            log.info("AES key recovered [aesKeyPath:{}, aesKeyHash:{}]",
                    aesKeyPath, BytesUtils.bytesToString(Hash.sha3(aesKey)));
        }

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
