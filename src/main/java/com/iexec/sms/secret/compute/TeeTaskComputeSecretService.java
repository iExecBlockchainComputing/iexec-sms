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

package com.iexec.sms.secret.compute;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeeTaskComputeSecretService {
    private final TeeTaskComputeSecretRepository teeTaskComputeSecretRepository;
    private final EncryptionService encryptionService;
    private final MeasuredSecretService measuredSecretService;

    protected TeeTaskComputeSecretService(TeeTaskComputeSecretRepository teeTaskComputeSecretRepository,
                                          EncryptionService encryptionService,
                                          MeasuredSecretService computeMeasuredSecretService) {
        this.teeTaskComputeSecretRepository = teeTaskComputeSecretRepository;
        this.encryptionService = encryptionService;
        this.measuredSecretService = computeMeasuredSecretService;
    }

    /**
     * Retrieve a secret.
     * Decrypt if required.
     */
    public Optional<TeeTaskComputeSecret> getSecret(
            OnChainObjectType onChainObjectType,
            String onChainObjectAddress,
            SecretOwnerRole secretOwnerRole,
            String secretOwner,
            String secretKey) {
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                onChainObjectType,
                onChainObjectAddress,
                secretOwnerRole,
                secretOwner,
                secretKey
        );
        final Optional<TeeTaskComputeSecret> oSecret = teeTaskComputeSecretRepository
                .findById(header);
        if (oSecret.isEmpty()) {
            return Optional.empty();
        }
        final TeeTaskComputeSecret secret = oSecret.get();
        final String decryptedValue = encryptionService.decrypt(secret.getValue());
        TeeTaskComputeSecret decryptedSecret = secret.withValue(decryptedValue);
        return Optional.of(decryptedSecret);
    }

    /**
     * Check whether a secret exists.
     *
     * @return {@code true} if the secret exists in the database, {@code false} otherwise.
     */
    public boolean isSecretPresent(OnChainObjectType onChainObjectType,
                                   String deployedObjectAddress,
                                   SecretOwnerRole secretOwnerRole,
                                   String secretOwner,
                                   String secretKey) {
        return getSecret(
                onChainObjectType,
                deployedObjectAddress,
                secretOwnerRole,
                secretOwner,
                secretKey
        ).isPresent();
    }

    /**
     * Encrypt a secret and store it if it doesn't already exist.
     *
     * @return {@code false} if the secret already exists, {@code true} otherwise.
     */
    public boolean encryptAndSaveSecret(OnChainObjectType onChainObjectType,
                                        String onChainObjectAddress,
                                        SecretOwnerRole secretOwnerRole,
                                        String secretOwner,
                                        String secretKey,
                                        String secretValue) {
        if (isSecretPresent(onChainObjectType, onChainObjectAddress, secretOwnerRole, secretOwner, secretKey)) {
            final TeeTaskComputeSecret secret = TeeTaskComputeSecret
                    .builder()
                    .onChainObjectType(onChainObjectType)
                    .onChainObjectAddress(onChainObjectAddress)
                    .secretOwnerRole(secretOwnerRole)
                    .fixedSecretOwner(secretOwner)
                    .key(secretKey)
                    .build();
            log.info("Tee task compute secret already exists, can't update it." +
                    " [secret:{}]", secret);
            return false;
        }
        final TeeTaskComputeSecret secret = TeeTaskComputeSecret
                .builder()
                .onChainObjectType(onChainObjectType)
                .onChainObjectAddress(onChainObjectAddress)
                .secretOwnerRole(secretOwnerRole)
                .fixedSecretOwner(secretOwner)
                .key(secretKey)
                .value(encryptionService.encrypt(secretValue))
                .build();
        log.info("Adding new tee task compute secret" +
                        " [secret:{}]", secret);
        teeTaskComputeSecretRepository.save(secret);
        measuredSecretService.newlyAddedSecret();
        return true;
    }
}
