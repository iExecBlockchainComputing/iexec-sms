/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.web3;

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
public class Web3SecretService {
    private final JdbcTemplate jdbcTemplate;
    private final Web3SecretRepository web3SecretRepository;
    private final EncryptionService encryptionService;
    private final MeasuredSecretService measuredSecretService;

    private final CacheSecretService<Web3SecretHeader> cacheSecretService;

    protected Web3SecretService(JdbcTemplate jdbcTemplate,
                                Web3SecretRepository web3SecretRepository,
                                EncryptionService encryptionService,
                                MeasuredSecretService web3MeasuredSecretService,
                                CacheSecretService<Web3SecretHeader> web3CacheSecretService) {
        this.jdbcTemplate = jdbcTemplate;
        this.web3SecretRepository = web3SecretRepository;
        this.encryptionService = encryptionService;
        this.measuredSecretService = web3MeasuredSecretService;
        this.cacheSecretService = web3CacheSecretService;
    }

    /**
     * Get the secret as it was saved in DB.
     * Its value should then be encrypted.
     *
     * @param secretAddress Address of the secret.
     * @return An empty {@link Optional} if no secret is found,
     * an {@link Optional} containing the secret if it exists.
     */
    Optional<Web3Secret> getSecret(String secretAddress) {
        return web3SecretRepository.findById(new Web3SecretHeader(secretAddress));
    }

    public Optional<String> getDecryptedValue(String secretAddress) {
        return getSecret(secretAddress)
                .map(secret -> encryptionService.decrypt(secret.getValue()));
    }

    public boolean isSecretPresent(String secretAddress) {
        final Web3SecretHeader key = new Web3SecretHeader(secretAddress);
        final Boolean found = cacheSecretService.lookSecretExistenceInCache(key);
        if (found != null) {
            return found;
        }
        final boolean isPresentInDB = getSecret(secretAddress).isPresent();
        cacheSecretService.putSecretExistenceInCache(key, isPresentInDB);
        return isPresentInDB;
    }

    /*
     *
     * Stores encrypted secrets
     * */
    public boolean addSecret(String secretAddress, String secretValue) {
        try {
            final String encryptedValue = encryptionService.encrypt(secretValue);
            log.info("Adding new web3 secret [secretAddress:{}, encryptedSecretValue:{}]",
                    secretAddress, encryptedValue);

            final Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedValue);
            final int result = jdbcTemplate.update("INSERT INTO \"web3secret\" (\"address\", \"value\") VALUES (?, ?)",
                    web3Secret.getHeader().getAddress(), web3Secret.getValue());
            // With SQL INSERT INTO and a single set VALUES, at most 1 row can be added and result can only be 0 or 1
            // When value should be 0, an exception should have been thrown
            // This check is only there as a fallback and cannot be reached in tests at the moment
            if (result != 1) {
                throw new RuntimeException("Data insert did not work but did not produce an exception");
            }
            cacheSecretService.putSecretExistenceInCache(web3Secret.getHeader(), true);
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
