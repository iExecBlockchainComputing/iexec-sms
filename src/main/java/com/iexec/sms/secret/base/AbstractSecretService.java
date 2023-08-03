/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.base;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.secret.exception.SecretAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

@Slf4j
public abstract class AbstractSecretService<S extends AbstractSecret<S, H>, H extends AbstractSecretHeader> {
    protected final CrudRepository<S, H> repository;
    protected final EncryptionService encryptionService;
    protected final MeasuredSecretService measuredSecretService;

    protected AbstractSecretService(CrudRepository<S, H> repository,
                                    EncryptionService encryptionService,
                                    MeasuredSecretService measuredSecretService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.measuredSecretService = measuredSecretService;
    }

    /**
     * Get the secret as it was saved in DB.
     * Its value should then be encrypted.
     *
     * @param header Header of the secret.
     * @return An empty {@link Optional} if no secret is found,
     * an {@link Optional} containing the secret if it exists.
     */
    protected Optional<S> getSecret(H header) {
        return repository.findById(header);
    }

    public Optional<String> getDecryptedValue(H header) {
        return getSecret(header)
                .map(secret -> encryptionService.decrypt(secret.getValue()));
    }

    public boolean isSecretPresent(H header) {
        return getSecret(header).isPresent();
    }

    /**
     * Creates and saves a new secret.
     * If a secret with same header already exists, then cancels the save.
     *
     * @param header      Header of the secret.
     * @param secretValue Unencrypted value of the secret.
     * @return {@literal true} if the operation succeeded,
     * {@literal false} otherwise.
     */
    public S addSecret(H header, String secretValue) throws SecretAlreadyExistsException {
        if (isSecretPresent(header)) {
            log.error("Secret already exists [header:{}]", header);
            throw new SecretAlreadyExistsException(header);
        }

        final String encryptedValue = encryptionService.encrypt(secretValue);
        final S newSecret = createSecret(header, encryptedValue);
        final S savedSecret = repository.save(newSecret);
        measuredSecretService.newlyAddedSecret();
        return savedSecret;
    }

    protected abstract S createSecret(H header, String value);
}
