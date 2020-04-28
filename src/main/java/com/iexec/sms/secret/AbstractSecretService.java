package com.iexec.sms.secret;

import com.iexec.sms.encryption.EncryptionService;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractSecretService {

    public EncryptionService encryptionService;

    public Secret encryptSecret(Secret secret) {
        if (!secret.isEncryptedValue()) {
            String encrypted = encryptionService.encrypt(secret.getValue());
            secret.setValue(encrypted, true);            
        }
        return secret;
    }

    public Secret decryptSecret(Secret secret) {
        if (secret.isEncryptedValue()) {
            String encrypted = encryptionService.decrypt(secret.getValue());
            secret.setValue(encrypted, false);
        }
        return secret;
    }

}