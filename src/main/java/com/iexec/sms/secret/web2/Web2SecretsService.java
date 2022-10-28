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

package com.iexec.sms.secret.web2;


import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.AbstractSecretService;
import com.iexec.sms.secret.Secret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class Web2SecretsService extends AbstractSecretService {

    private final Web2SecretsRepository web2SecretsRepository;

    public Web2SecretsService(Web2SecretsRepository web2SecretsRepository,
                              EncryptionService encryptionService) {
        super(encryptionService);
        this.web2SecretsRepository = web2SecretsRepository;
    }

    public Optional<Web2Secrets> getWeb2Secrets(String ownerAddress) {
        ownerAddress = ownerAddress.toLowerCase();
        return web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress);
    }

    public Optional<Secret> getSecret(String ownerAddress, String secretAddress) {
        return getSecret(ownerAddress, secretAddress, false);
    }

    public Optional<Secret> getSecret(String ownerAddress, String secretAddress, boolean shouldDecryptValue) {
        ownerAddress = ownerAddress.toLowerCase();
        Optional<Secret> oSecret = getWeb2Secrets(ownerAddress)
                .flatMap(web2Secrets -> web2Secrets.getSecret(secretAddress));

        if (oSecret.isEmpty() || !shouldDecryptValue) {
            return oSecret;
        }

        final Secret secret = oSecret.get();
        final String decryptedValue = encryptionService.decrypt(secret.getValue());
        return Optional.of(secret.withValue(decryptedValue, false));
    }

    public Optional<Secret> addSecret(String ownerAddress, String secretAddress, String secretValue) {
        ownerAddress = ownerAddress.toLowerCase();
        final Web2Secrets web2Secrets = getWeb2Secrets(ownerAddress)
                .orElse(new Web2Secrets(ownerAddress));

        final String encryptedValue = encryptionService.encrypt(secretValue);
        log.info("Adding new secret [ownerAddress:{}, secretAddress:{}, encryptedSecretValue:{}]",
                ownerAddress, secretAddress, encryptedValue);

        try {
            final Web2Secrets newWeb2Secrets = web2Secrets.addNewSecret(secretAddress, encryptedValue, true);
            web2SecretsRepository.save(newWeb2Secrets);
            return newWeb2Secrets.getSecret(secretAddress);
        } catch (SecretAlreadyExistsException e) {
            log.error("Secret already exists [ownerAddress:{}, secretAddress:{}]", ownerAddress, secretAddress, e);
            return Optional.empty();
        }
    }

    public Optional<Secret> updateSecret(String ownerAddress, String secretAddress, String newSecretValue) throws NotAnExistingSecretException {
        ownerAddress = ownerAddress.toLowerCase();
        final Web2Secrets web2Secrets = getWeb2Secrets(ownerAddress).orElseThrow();
        final String encryptedValue = encryptionService.encrypt(newSecretValue);
        log.info("Updating secret [ownerAddress:{}, secretAddress:{}, newEncryptedSecretValue:{}]",
                ownerAddress, secretAddress, encryptedValue);

        try {
            final Web2Secrets newWeb2Secrets = web2Secrets.updateSecret(secretAddress, encryptedValue, true);
            web2SecretsRepository.save(newWeb2Secrets);
            return newWeb2Secrets.getSecret(secretAddress);
        } catch (NotAnExistingSecretException e) {
            log.error("Secret does not exist, can't update it [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, secretAddress, e);
            throw e;
        } catch (SameSecretException e) {
            log.info("No need to update secret [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, secretAddress);
            return Optional.empty();
        }
    }
}
