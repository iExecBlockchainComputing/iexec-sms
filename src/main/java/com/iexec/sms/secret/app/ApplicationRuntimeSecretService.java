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

package com.iexec.sms.secret.app;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.AbstractSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ApplicationRuntimeSecretService extends AbstractSecretService {
    private final ApplicationRuntimeSecretRepository applicationRuntimeSecretRepository;

    public ApplicationRuntimeSecretService(ApplicationRuntimeSecretRepository applicationRuntimeSecretRepository,
                                           EncryptionService encryptionService) {
        super(encryptionService);
        this.applicationRuntimeSecretRepository = applicationRuntimeSecretRepository;
    }

    /**
     * Retrieve a secret identified by its app address and its index.
     * Decrypt it if required and if its decryption is required.
     */
    public Optional<ApplicationRuntimeSecret> getSecret(String appAddress,
                                                        long secretIndex,
                                                        boolean shouldDecryptValue) {
        appAddress = appAddress.toLowerCase();
        Optional<ApplicationRuntimeSecret> secret =
                applicationRuntimeSecretRepository.findByAddressAndIndex(appAddress, secretIndex);
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
    public Optional<ApplicationRuntimeSecret> getSecret(String appAddress, long secretIndex) {
        return getSecret(appAddress, secretIndex, false);
    }

    /**
     * Check whether a secret exists.
     * @return {@code true} if the secret exists in the database, {@code false} otherwise.
     */
    public boolean isSecretPresent(String appAddress, long secretIndex) {
        return getSecret(appAddress, secretIndex).isPresent();
    }

    /**
     * Stores encrypted secrets.
     */
    public void encryptAndSaveSecret(String appAddress, long secretIndex, String secretValue) {
        appAddress = appAddress.toLowerCase();
        ApplicationRuntimeSecret applicationRuntimeSecret =
                new ApplicationRuntimeSecret(appAddress, secretIndex, secretValue);
        encryptSecret(applicationRuntimeSecret);
        log.info("Adding new app runtime secret [appAddress:{}, secretIndex:{}, encryptedSecretValue:{}]",
                appAddress, secretIndex, applicationRuntimeSecret.getValue());
        applicationRuntimeSecretRepository.save(applicationRuntimeSecret);
    }
}
