package com.iexec.sms.iexecsms.encryption;


import com.iexec.common.security.CipherHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EncryptionService {

    private EncryptionConfiguration configuration;

    public EncryptionService(EncryptionConfiguration configuration) {
        this.configuration = configuration;

        //TODO Get or create key here

        if (configuration.getAesKey() == null || configuration.getAesKey().isEmpty()) {
            throw new ExceptionInInitializerError("Aes key for storage encryption missing");
        }
    }

    private byte[] getAesKey() {
        return configuration.getAesKey().getBytes();
    }

    public String encrypt(String data) {
        byte[] encryptedData = CipherHelper.aesEncrypt(data.getBytes(), getAesKey());
        if (encryptedData != null){
            return new String(encryptedData);
        }
        return "";
    }

    public String decrypt(String encryptedData) {
        byte[] decryptedData = CipherHelper.aesDecrypt(encryptedData.getBytes(), getAesKey());
        if (decryptedData != null){
            return new String(decryptedData);
        }
        return "";
    }

}
