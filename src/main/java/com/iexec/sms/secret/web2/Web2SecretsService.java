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
        ownerAddress = ownerAddress.toLowerCase();
        return getWeb2Secrets(ownerAddress)
                .flatMap(web2Secrets -> web2Secrets.getSecret(secretAddress));
    }

    public Optional<Secret> getSecret(String ownerAddress, String secretAddress, boolean shouldDecryptValue) {
        Optional<Secret> oSecret = getSecret(ownerAddress, secretAddress);
        return shouldDecryptValue ? oSecret.map(this::decryptSecret) : oSecret;
    }

    public boolean addSecret(String ownerAddress, String secretAddress, String secretValue) {
        ownerAddress = ownerAddress.toLowerCase();
        Web2Secrets web2Secrets = getWeb2Secrets(ownerAddress)
                .orElse(new Web2Secrets(ownerAddress));

        if (web2Secrets.getSecret(secretAddress).isPresent()) {
            log.error("Secret already exists [ownerAddress:{}, secretAddress:{}]", ownerAddress, secretAddress);
            return false;
        }

        Secret secret = new Secret(secretAddress, secretValue);
        encryptSecret(secret);
        log.info("Adding new secret [ownerAddress:{}, secretAddress:{}, encryptedSecretValue:{}]",
                ownerAddress, secretAddress, secret.getValue());
        web2Secrets.getSecrets().add(secret);
        web2SecretsRepository.save(web2Secrets);
        return true;
    }

    public void updateSecret(String ownerAddress, String secretAddress, String newSecretValue) {
        ownerAddress = ownerAddress.toLowerCase();
        Secret newSecret = new Secret(secretAddress, newSecretValue);
        encryptSecret(newSecret);
        Web2Secrets web2Secrets = getWeb2Secrets(ownerAddress).orElseThrow();
        Secret existingSecret = web2Secrets.getSecret(secretAddress).orElseThrow();
        if (existingSecret.getValue().equals(newSecret.getValue())) {
            log.info("No need to update secret [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, secretAddress);
            return;
        }

        log.info("Updating secret [ownerAddress:{}, secretAddress:{}, oldEncryptedSecretValue:{}, newEncryptedSecretValue:{}]",
                ownerAddress, secretAddress, existingSecret.getValue(), newSecret.getValue());
        existingSecret.setValue(newSecret.getValue(), true);
        web2SecretsRepository.save(web2Secrets);
    }
}
