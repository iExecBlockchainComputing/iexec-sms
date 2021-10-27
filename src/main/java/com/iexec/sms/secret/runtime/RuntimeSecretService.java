/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.runtime;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.AbstractSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class RuntimeSecretService extends AbstractSecretService {
    private final RuntimeSecretRepository runtimeSecretRepository;

    public RuntimeSecretService(RuntimeSecretRepository runtimeSecretRepository,
                                EncryptionService encryptionService) {
        super(encryptionService);
        this.runtimeSecretRepository = runtimeSecretRepository;
    }

    /**
     * Retrieve a secret identified by its app address and its index.
     * Decrypt it if required and if its decryption is required.
     */
    public Optional<RuntimeSecret> getSecret(String secretAppAddress, long secretIndex, boolean shouldDecryptValue) {
        secretAppAddress = secretAppAddress.toLowerCase();
        Optional<RuntimeSecret> secret = runtimeSecretRepository.findByAddressAndIndex(secretAppAddress, secretIndex);
        if (secret.isEmpty()) {
            return Optional.empty();
        }
        if (shouldDecryptValue) {
            decryptSecret(secret.get());
        }
        return secret;
    }

    /**
     * Retrieve a secret identified by its app address and its index.
     */
    public Optional<RuntimeSecret> getSecret(String secretAppAddress, long secretIndex) {
        secretAppAddress = secretAppAddress.toLowerCase();
        return getSecret(secretAppAddress, secretIndex, false);
    }

    /**
     * Stores encrypted secrets.
     */
    public void addSecret(String secretAppAddress, long secretIndex, String secretValue) {
        secretAppAddress = secretAppAddress.toLowerCase();
        RuntimeSecret runtimeSecret = new RuntimeSecret(secretAppAddress, secretIndex, secretValue);
        encryptSecret(runtimeSecret);
        log.info("Adding new runtime secret [secretAppAddress:{}, secretIndex:{}, secretValueHash:{}]",
                secretAppAddress, secretIndex, runtimeSecret.getValue());
        runtimeSecretRepository.save(runtimeSecret);
    }
}
