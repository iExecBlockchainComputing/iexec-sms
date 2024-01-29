/*
 *
 *  * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.iexec.sms.secret.web2;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.AbstractSecretService;
import com.iexec.sms.secret.MeasuredSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class Web2SecretService extends AbstractSecretService<Web2SecretHeader> {

    private final Web2SecretRepository web2SecretRepository;
    private final EncryptionService encryptionService;
    private final MeasuredSecretService measuredSecretService;


    protected Web2SecretService(Web2SecretRepository web2SecretRepository,
                                EncryptionService encryptionService,
                                MeasuredSecretService web2MeasuredSecretService) {
        this.web2SecretRepository = web2SecretRepository;
        this.encryptionService = encryptionService;
        this.measuredSecretService = web2MeasuredSecretService;
    }

    /**
     * Get the secret as it was saved in DB.
     * Its value should then be encrypted.
     *
     * @param ownerAddress  Address of the secret owner.
     * @param secretAddress Address of the secret.
     * @return An empty {@link Optional} if no secret is found,
     * an {@link Optional} containing the secret if it exists.
     */
    Optional<Web2Secret> getSecret(String ownerAddress, String secretAddress) {
        return web2SecretRepository.findById(new Web2SecretHeader(ownerAddress, secretAddress));
    }

    public Optional<String> getDecryptedValue(String ownerAddress, String secretAddress) {
        return getSecret(ownerAddress, secretAddress)
                .map(secret -> encryptionService.decrypt(secret.getValue()));
    }

    public boolean isSecretPresent(String ownerAddress, String secretAddress) {
        final Web2SecretHeader key = new Web2SecretHeader(ownerAddress, secretAddress);
        if (lookSecretExistenceInCache(key)) {
            return true;
        }
        if (getSecret(ownerAddress, secretAddress).isPresent()) {
            putSecretExistenceInCache(key);
            return true;
        }
        return false;
    }

    /**
     * Creates and saves a new {@link Web2Secret}.
     * If a secret with same {@code ownerAddress}/{@code secretAddress} couple already exists, then cancels the save.
     *
     * @param ownerAddress  Address of the secret owner.
     * @param secretAddress Address of the secret.
     * @param secretValue   Unencrypted value of the secret.
     * @return The {@link Web2Secret} that has been saved.
     * @throws SecretAlreadyExistsException throw when a secret
     *                                      with same {@code ownerAddress}/{@code secretAddress} couple already exists
     */
    public Web2Secret addSecret(String ownerAddress, String secretAddress, String secretValue) throws SecretAlreadyExistsException {
        if (isSecretPresent(ownerAddress, secretAddress)) {
            log.error("Secret already exists [ownerAddress:{}, secretAddress:{}]", ownerAddress, secretAddress);
            throw new SecretAlreadyExistsException(ownerAddress, secretAddress);
        }

        final String encryptedValue = encryptionService.encrypt(secretValue);
        final Web2Secret newSecret = new Web2Secret(ownerAddress, secretAddress, encryptedValue);
        final Web2Secret savedSecret = web2SecretRepository.save(newSecret);
        putSecretExistenceInCache(savedSecret.getHeader());
        measuredSecretService.newlyAddedSecret();
        return savedSecret;
    }

    /**
     * Updates an existing {@link Web2Secret}.
     * If the secret does not already exist, then cancels the save.
     * If the secret already exists with the same encrypted value, then cancels the save.
     *
     * @param ownerAddress   Address of the secret owner.
     * @param secretAddress  Address of the secret.
     * @param newSecretValue New, unencrypted value of the secret.
     * @return The {@link Web2Secret} that has been saved.
     * @throws NotAnExistingSecretException thrown when the requested secret does not exist.
     * @throws SameSecretException          thrown when the requested secret already contains the encrypted value.
     */
    public Web2Secret updateSecret(String ownerAddress, String secretAddress, String newSecretValue) throws NotAnExistingSecretException, SameSecretException {
        final Optional<Web2Secret> oSecret = getSecret(ownerAddress, secretAddress);
        if (oSecret.isEmpty()) {
            log.error("Secret does not exist, can't update it [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, secretAddress);
            throw new NotAnExistingSecretException(ownerAddress, secretAddress);
        }

        final Web2Secret secret = oSecret.get();
        final String encryptedValue = encryptionService.encrypt(newSecretValue);
        if (Objects.equals(secret.getValue(), encryptedValue)) {
            log.info("No need to update secret [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, secretAddress);
            throw new SameSecretException(ownerAddress, secretAddress);
        }

        final Web2Secret newSecret = secret.withValue(encryptedValue);
        final Web2Secret savedSecret = web2SecretRepository.save(newSecret);
        putSecretExistenceInCache(savedSecret.getHeader());
        return savedSecret;
    }
}
