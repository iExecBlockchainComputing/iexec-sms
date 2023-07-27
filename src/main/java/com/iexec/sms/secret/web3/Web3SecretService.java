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

package com.iexec.sms.secret.web3;


import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.AbstractSecretService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Slf4j
@Service
public class Web3SecretService extends AbstractSecretService {
    private static final String METRICS_PREFIX = "iexec.sms.secrets.web3.";

    private final Web3SecretRepository web3SecretRepository;
    private final Counter addedSecretsSinceStart = Metrics.counter(METRICS_PREFIX + "added");

    public Web3SecretService(Web3SecretRepository web3SecretRepository,
                             EncryptionService encryptionService) {
        super(encryptionService);
        this.web3SecretRepository = web3SecretRepository;

        Metrics.gauge(METRICS_PREFIX + "stored", web3SecretRepository, Web3SecretRepository::count);
    }

    @PostConstruct
    void init() {
        final long initialSecretsCount = web3SecretRepository.count();
        Metrics.counter(METRICS_PREFIX + "initial").increment(initialSecretsCount);
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
        return getSecret(secretAddress).isPresent();
    }

    /*
     *
     * Stores encrypted secrets
     * */
    public boolean addSecret(String secretAddress, String secretValue) {
        if (isSecretPresent(secretAddress)) {
            log.error("Secret already exists [secretAddress:{}]", secretAddress);
            return false;
        }

        final String encryptedValue = encryptionService.encrypt(secretValue);
        log.info("Adding new web3 secret [secretAddress:{}, encryptedSecretValue:{}]",
                secretAddress, encryptedValue);

        final Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedValue);
        web3SecretRepository.save(web3Secret);
        addedSecretsSinceStart.increment();
        return true;
    }

}
