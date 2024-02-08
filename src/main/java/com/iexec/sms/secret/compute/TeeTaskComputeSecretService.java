/*
 * Copyright 2021-2024 IEXEC BLOCKCHAIN TECH
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
import com.iexec.sms.secret.CacheSecretService;
import com.iexec.sms.secret.MeasuredSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeeTaskComputeSecretService {
    private final JdbcTemplate jdbcTemplate;
    private final TeeTaskComputeSecretRepository teeTaskComputeSecretRepository;
    private final EncryptionService encryptionService;
    private final MeasuredSecretService measuredSecretService;
    private final CacheSecretService<TeeTaskComputeSecretHeader> cacheSecretService;

    protected TeeTaskComputeSecretService(JdbcTemplate jdbcTemplate,
                                          TeeTaskComputeSecretRepository teeTaskComputeSecretRepository,
                                          EncryptionService encryptionService,
                                          MeasuredSecretService computeMeasuredSecretService,
                                          CacheSecretService<TeeTaskComputeSecretHeader> teeTaskComputeCacheSecretService) {
        this.jdbcTemplate = jdbcTemplate;
        this.teeTaskComputeSecretRepository = teeTaskComputeSecretRepository;
        this.encryptionService = encryptionService;
        this.measuredSecretService = computeMeasuredSecretService;
        this.cacheSecretService = teeTaskComputeCacheSecretService;
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

        final TeeTaskComputeSecretHeader key = new TeeTaskComputeSecretHeader(
                onChainObjectType,
                deployedObjectAddress,
                secretOwnerRole,
                secretOwner,
                secretKey
        );
        final Boolean found = cacheSecretService.lookSecretExistenceInCache(key);
        if (found != null) {
            return found;
        }

        final boolean isPresentInDB = getSecret(
                onChainObjectType,
                deployedObjectAddress,
                secretOwnerRole,
                secretOwner,
                secretKey
        ).isPresent();

        cacheSecretService.putSecretExistenceInCache(key, isPresentInDB);
        return isPresentInDB;
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
        try {
            final TeeTaskComputeSecret secret = TeeTaskComputeSecret
                    .builder()
                    .onChainObjectType(onChainObjectType)
                    .onChainObjectAddress(onChainObjectAddress)
                    .secretOwnerRole(secretOwnerRole)
                    .fixedSecretOwner(secretOwner)
                    .key(secretKey)
                    .value(encryptionService.encrypt(secretValue))
                    .build();
            log.info("Adding new tee task compute secret [secret:{}]", secret);
            final int result = jdbcTemplate.update("INSERT INTO \"tee_task_compute_secret\" "
                            + "(\"on_chain_object_type\", \"on_chain_object_address\", \"secret_owner_role\", \"fixed_secret_owner\", \"key\", \"value\") VALUES "
                            + "(?, ?, ?, ?, ?, ?)",
                    secret.getHeader().getOnChainObjectType().ordinal(), secret.getHeader().getOnChainObjectAddress(),
                    secret.getHeader().getSecretOwnerRole().ordinal(), secret.getHeader().getFixedSecretOwner(),
                    secret.getHeader().getKey(), secret.getValue());
            // With SQL INSERT INTO and a single set VALUES, at most 1 row can be added and result can only be 0 or 1
            // When value should be 0, an exception should have been thrown
            // This check is only there as a fallback and cannot be reached in tests at the moment
            if (result != 1) {
                throw new RuntimeException("Data insert did not work but did not produce an exception");
            }
            cacheSecretService.putSecretExistenceInCache(secret.getHeader(), true);
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
}
