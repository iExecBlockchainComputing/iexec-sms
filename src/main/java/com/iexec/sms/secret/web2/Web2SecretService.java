/*
 *
 *  * Copyright 2022 IEXEC BLOCKCHAIN TECH
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
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.secret.base.AbstractSecretService;
import com.iexec.sms.secret.exception.NotAnExistingSecretException;
import com.iexec.sms.secret.exception.SameSecretException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class Web2SecretService extends AbstractSecretService<Web2Secret, Web2SecretHeader> {
    protected Web2SecretService(Web2SecretRepository web2SecretRepository,
                                EncryptionService encryptionService,
                                MeasuredSecretService web2MeasuredSecretService) {
        super(web2SecretRepository, encryptionService, web2MeasuredSecretService);
    }

    @Override
    protected Web2Secret createSecret(Web2SecretHeader header, String value) {
        return new Web2Secret(header, value);
    }

    /**
     * Updates an existing secret.
     * If the secret does not already exist, then cancels the save.
     * If the secret already exists with the same encrypted value, then cancels the save.
     *
     * @param header         Header of the secret.
     * @param newSecretValue New, unencrypted value of the secret.
     * @return The secret that has been saved.
     * @throws NotAnExistingSecretException thrown when the requested secret does not exist.
     * @throws SameSecretException          thrown when the requested secret already contains the encrypted value.
     */
    public Web2Secret updateSecret(Web2SecretHeader header, String newSecretValue) throws NotAnExistingSecretException, SameSecretException {
        final Optional<Web2Secret> oSecret = getSecret(header);
        if (oSecret.isEmpty()) {
            log.error("Secret does not exist, can't update it [header:{}]", header);
            throw new NotAnExistingSecretException(header);
        }

        final Web2Secret secret = oSecret.get();
        final String encryptedValue = encryptionService.encrypt(newSecretValue);
        if (Objects.equals(secret.getValue(), encryptedValue)) {
            log.info("No need to update secret [header:{}]", header);
            throw new SameSecretException(header);
        }

        final Web2Secret newSecret = createSecret(header, encryptedValue);
        return repository.save(newSecret);
    }
}
