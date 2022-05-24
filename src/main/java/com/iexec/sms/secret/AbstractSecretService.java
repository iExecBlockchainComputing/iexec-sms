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

package com.iexec.sms.secret;

import com.iexec.sms.encryption.EncryptionService;

public abstract class AbstractSecretService {

    private final EncryptionService encryptionService;

    public AbstractSecretService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public Secret encryptSecret(Secret secret) {
        if (!secret.isEncryptedValue()) {
            String encrypted = encryptionService.encrypt(secret.getValue());
            secret.setValue(encrypted, true);            
        }
        return secret;
    }

    public Secret decryptSecret(Secret secret) {
        if (secret.isEncryptedValue()) {
            String decrypted = encryptionService.decrypt(secret.getValue());
            secret.setValue(decrypted, false);
        }
        return secret;
    }

}