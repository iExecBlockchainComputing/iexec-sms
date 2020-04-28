package com.iexec.sms.secret.web2;


import com.iexec.sms.secret.AbstractSecretService;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.encryption.EncryptionService;
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

    private Web2Secrets getWeb2Secrets(String ownerAddress, boolean shouldDecryptValues) {
        Optional<Web2Secrets> oldWeb2Secrets = web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress);
        if (!oldWeb2Secrets.isPresent()) {
            return web2SecretsRepository.save(new Web2Secrets(ownerAddress));
        }

        if (shouldDecryptValues){
            for (Secret secret: oldWeb2Secrets.get().getSecrets()){
                encryptSecret(secret);
            }
        }

        return oldWeb2Secrets.get();
    }

    public Optional<Secret> getSecret(String ownerAddress, String secretAddress, boolean shouldDecryptValue) {
        Web2Secrets web2Secrets = getWeb2Secrets(ownerAddress, shouldDecryptValue);
        Secret secret = web2Secrets.getSecret(secretAddress);
        if (secret == null) {
            return Optional.empty();
        }
        return shouldDecryptValue ? Optional.of(decryptSecret(secret)) : Optional.of(secret);
    }

    public boolean addSecret(String ownerAddress, Secret secret) {
        Web2Secrets web2Secrets = new Web2Secrets(ownerAddress);
        Optional<Web2Secrets> existingWeb2Secrets = web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress);
        if (existingWeb2Secrets.isPresent()) {
            web2Secrets = existingWeb2Secrets.get();
        }

        if (web2Secrets.getSecret(secret.getAddress()) == null) {
            log.error("Secret for this address already exists [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, secret.getAddress());
            return false;
        }

        encryptSecret(secret);
        log.info("Adding new secret [ownerAddress:{}, secretAddress:{}, secretValueHash:{}]",
                ownerAddress, secret.getAddress(), secret.getValue());
        web2Secrets.getSecrets().add(secret);
        web2SecretsRepository.save(web2Secrets);
        return true;
    }

    public boolean updateSecret(String ownerAddress, Secret newSecret) {
        Optional<Web2Secrets> web2Secrets = web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress);
        if (web2Secrets.isEmpty() || web2Secrets.get().getSecret(newSecret.getAddress()) == null) {
            log.error("Secret not found [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, newSecret.getAddress());
            return false;
        }

        Secret existingSecret = web2Secrets.get().getSecret(newSecret.getAddress());
        if (existingSecret.getValue().equals(newSecret.getValue())) {
            log.info("No need to update secret [ownerAddress:{}, secretAddress:{}]",
                    ownerAddress, newSecret.getAddress());
            return true;
        }

        encryptSecret(newSecret);
        log.info("Updating secret [ownerAddress:{}, secretAddress:{}, oldSecretValueHash:{}, newSecretValueHash:{}]",
                ownerAddress, newSecret.getAddress(), existingSecret.getValue(), newSecret.getValue());
        existingSecret.setValue(newSecret.getValue(), true);
        web2SecretsRepository.save(web2Secrets.get());
        return true;
    }
}
