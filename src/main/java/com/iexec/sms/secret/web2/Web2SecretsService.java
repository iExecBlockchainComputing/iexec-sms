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

    private Web2SecretsRepository web2SecretsRepository;

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
        return getSecret(ownerAddress, secretAddress, false);
    }

    public Optional<Secret> getSecret(String ownerAddress, String secretAddress, boolean shouldDecryptValue) {
        ownerAddress = ownerAddress.toLowerCase();
        Optional<Web2Secrets> web2Secrets = getWeb2Secrets(ownerAddress);
        if (!web2Secrets.isPresent()) {
            return Optional.empty();
        }
        Secret secret = web2Secrets.get().getSecret(secretAddress);
        if (secret == null) {
            return Optional.empty();
        }
        if (shouldDecryptValue) {
            decryptSecret(secret);
        }
        return Optional.of(secret);
    }

    public void addSecret(String ownerAddress, String secretAddress, String secretValue) {
        ownerAddress = ownerAddress.toLowerCase();
        Web2Secrets web2Secrets = new Web2Secrets(ownerAddress);
        Optional<Web2Secrets> existingWeb2Secrets = getWeb2Secrets(ownerAddress);
        if (existingWeb2Secrets.isPresent()) {
            web2Secrets = existingWeb2Secrets.get();
        }

        Secret secret = new Secret(secretAddress, secretValue);
        encryptSecret(secret);
        log.info("Adding new secret [ownerAddress:{}, secretAddress:{}, secretValueHash:{}]",
                ownerAddress, secretAddress, secret.getValue());
        web2Secrets.getSecrets().add(secret);
        web2SecretsRepository.save(web2Secrets);
    }

    public void updateSecret(String ownerAddress, String secretAddress, String newSecretValue) {
        ownerAddress = ownerAddress.toLowerCase();
        Secret newSecret = new Secret(secretAddress, newSecretValue);
        encryptSecret(newSecret);
        Optional<Web2Secrets> web2Secrets = getWeb2Secrets(ownerAddress);
        Secret existingSecret = web2Secrets.get().getSecret(secretAddress);
        if (existingSecret.getValue().equals(newSecret.getValue())) {
            log.info("No need to update secret [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, secretAddress);
            return;
        }

        log.info("Updating secret [ownerAddress:{}, secretAddress:{}, oldSecretValueHash:{}, newSecretValueHash:{}]",
                ownerAddress, secretAddress, existingSecret.getValue(), newSecret.getValue());
        existingSecret.setValue(newSecret.getValue(), true);
        web2SecretsRepository.save(web2Secrets.get());
    }
}
