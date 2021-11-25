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
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeeTaskRuntimeSecretService extends AbstractSecretService {
    private final TeeTaskRuntimeSecretRepository teeTaskRuntimeSecretRepository;

    protected TeeTaskRuntimeSecretService(
            TeeTaskRuntimeSecretRepository teeTaskRuntimeSecretRepository,
            EncryptionService encryptionService) {
        super(encryptionService);
        this.teeTaskRuntimeSecretRepository = teeTaskRuntimeSecretRepository;
    }

    /**
     * Retrieve a secret.
     * Decrypt it's encrypted and if its decryption is required.
     */
    public Optional<TeeTaskRuntimeSecret> getSecret(
            DeployedObjectType deployedObjectType,
            String deployedObjectAddress,
            OwnerRole secretOwnerRole,
            String owner,
            long secretIndex,
            boolean shouldDecryptValue) {
        deployedObjectAddress = deployedObjectAddress.toLowerCase();
        final TeeTaskRuntimeSecret wantedSecret = new TeeTaskRuntimeSecret(
                deployedObjectType,
                deployedObjectAddress,
                secretOwnerRole,
                owner,
                secretIndex,
                null
        );
        final ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withIgnorePaths("value")
                .withIgnorePaths("isEncryptedValue");
        final Optional<TeeTaskRuntimeSecret> secret = teeTaskRuntimeSecretRepository
                .findOne(Example.of(wantedSecret, exampleMatcher));
        if (secret.isEmpty()) {
            return Optional.empty();
        }
        if (shouldDecryptValue) {
            decryptSecret(secret.get());
        }
        return secret;
    }

    /**
     * Check whether a secret exists.
     *
     * @return {@code true} if the secret exists in the database, {@code false} otherwise.
     */
    public boolean isSecretPresent(DeployedObjectType deployedObjectType,
                                   String deployedObjectAddress,
                                   OwnerRole secretOwnerRole,
                                   String owner,
                                   long secretIndex) {
        return getSecret(
                deployedObjectType,
                deployedObjectAddress,
                secretOwnerRole,
                owner,
                secretIndex,
                false
        ).isPresent();
    }

    /**
     * Stores encrypted secrets.
     */
    public void encryptAndSaveSecret(DeployedObjectType deployedObjectType,
                                     String deployedObjectAddress,
                                     OwnerRole secretOwnerRole,
                                     String owner,
                                     long secretIndex,
                                     String secretValue) {
        deployedObjectAddress = deployedObjectAddress.toLowerCase();
        TeeTaskRuntimeSecret secret = new TeeTaskRuntimeSecret(
                deployedObjectType,
                deployedObjectAddress,
                secretOwnerRole,
                owner,
                secretIndex, secretValue
        );
        encryptSecret(secret);
        log.info("Adding new tee task runtime secret " +
                        "[secret:{}]", secret);
        teeTaskRuntimeSecretRepository.save(secret);
    }
}
