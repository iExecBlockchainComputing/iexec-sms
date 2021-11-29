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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeeTaskRuntimeSecretService {
    private final TeeTaskRuntimeSecretRepository teeTaskRuntimeSecretRepository;
    private final EncryptionService encryptionService;

    protected TeeTaskRuntimeSecretService(
            TeeTaskRuntimeSecretRepository teeTaskRuntimeSecretRepository,
            EncryptionService encryptionService) {
        this.teeTaskRuntimeSecretRepository = teeTaskRuntimeSecretRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Retrieve a secret.
     * Decrypt if required.
     */
    public Optional<TeeTaskRuntimeSecret> getSecret(
            DeployedObjectType deployedObjectType,
            String deployedObjectAddress,
            OwnerRole secretOwnerRole,
            String secretOwner,
            long secretIndex,
            boolean shouldDecryptValue) {
        deployedObjectAddress = deployedObjectAddress.toLowerCase();
        final TeeTaskRuntimeSecret wantedSecret = new TeeTaskRuntimeSecret(
                deployedObjectType,
                deployedObjectAddress,
                secretOwnerRole,
                secretOwner,
                secretIndex,
                null
        );
        final ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withIgnorePaths("value");
        final Optional<TeeTaskRuntimeSecret> oSecret = teeTaskRuntimeSecretRepository
                .findOne(Example.of(wantedSecret, exampleMatcher));
        if (oSecret.isEmpty()) {
            return Optional.empty();
        }
        if (shouldDecryptValue) {
            final TeeTaskRuntimeSecret secret = oSecret.get();
            final String decryptedValue = encryptionService.decrypt(secret.getValue());
            secret.setValue(decryptedValue);
        }
        return oSecret;
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
     * Encrypt secrets and store them.
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
                secretIndex,
                encryptionService.encrypt(secretValue)
        );
        log.info("Adding new tee task runtime secret " +
                        "[secret:{}]", secret);
        teeTaskRuntimeSecretRepository.save(secret);
    }
}
