/*
 * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.web2;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.CacheSecretService;
import com.iexec.sms.secret.MeasuredSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class Web2SecretService {
    private final JdbcTemplate jdbcTemplate;
    private final Web2SecretRepository web2SecretRepository;
    private final EncryptionService encryptionService;
    private final MeasuredSecretService measuredSecretService;
    private final CacheSecretService<Web2SecretHeader> cacheSecretService;

    protected Web2SecretService(JdbcTemplate jdbcTemplate,
                                Web2SecretRepository web2SecretRepository,
                                EncryptionService encryptionService,
                                MeasuredSecretService web2MeasuredSecretService,
                                CacheSecretService<Web2SecretHeader> web2CacheSecretService) {
        this.jdbcTemplate = jdbcTemplate;
        this.web2SecretRepository = web2SecretRepository;
        this.encryptionService = encryptionService;
        this.measuredSecretService = web2MeasuredSecretService;
        this.cacheSecretService = web2CacheSecretService;
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
        final Boolean found = cacheSecretService.lookSecretExistenceInCache(key);
        if (found != null) {
            return found;
        }
        final boolean isPresentInDB = getSecret(ownerAddress, secretAddress).isPresent();
        cacheSecretService.putSecretExistenceInCache(key, isPresentInDB);
        return isPresentInDB;
    }

    /**
     * Creates and saves a new {@link Web2Secret}.
     * If a secret with same {@code ownerAddress}/{@code secretAddress} couple already exists, then cancels the save.
     *
     * @param ownerAddress  Address of the secret owner.
     * @param secretAddress Address of the secret.
     * @param secretValue   Unencrypted value of the secret.
     * @return The {@link Web2Secret} that has been saved.
     */
    public boolean addSecret(String ownerAddress, String secretAddress, String secretValue) {
        try {
            final String encryptedValue = encryptionService.encrypt(secretValue);
            final Web2Secret web2Secret = new Web2Secret(ownerAddress, secretAddress, encryptedValue);
            final int result = jdbcTemplate.update("INSERT INTO \"web2secret\" (\"owner_address\", \"address\", \"value\") VALUES (?, ?, ?)",
                    web2Secret.getHeader().getOwnerAddress(), web2Secret.getHeader().getAddress(), web2Secret.getValue());
            // With SQL INSERT INTO and a single set VALUES, at most 1 row can be added and result can only be 0 or 1
            // When value should be 0, an exception should have been thrown
            // This check is only there as a fallback and cannot be reached in tests at the moment
            if (result != 1) {
                throw new IncorrectResultSizeDataAccessException("Data insert did not work but did not produce an exception", 1);
            }
            cacheSecretService.putSecretExistenceInCache(web2Secret.getHeader(), true);
            measuredSecretService.newlyAddedSecret();
            return true;
        } catch (DuplicateKeyException e) {
            log.debug(e.getMostSpecificCause().getMessage());
        } catch (DataAccessException e) {
            log.error(e.getMostSpecificCause().getMessage());
        } catch (Exception e) {
            log.error("Data insert failed with message {}", e.getMessage());
        }
        return false;
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
        cacheSecretService.putSecretExistenceInCache(savedSecret.getHeader(), true);
        return savedSecret;
    }
}
