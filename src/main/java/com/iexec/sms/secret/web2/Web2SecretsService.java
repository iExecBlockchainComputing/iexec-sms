package com.iexec.sms.secret.web2;


import com.iexec.sms.secret.Secret;
import com.iexec.sms.encryption.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class Web2SecretsService {

    private Web2SecretsRepository web2SecretsRepository;
    private EncryptionService encryptionService;

    public Web2SecretsService(Web2SecretsRepository web2SecretsRepository,
                              EncryptionService encryptionService) {
        this.web2SecretsRepository = web2SecretsRepository;
        this.encryptionService = encryptionService;
    }

    public Optional<Web2Secrets> getWeb2Secrets(String address, boolean shouldDecryptValues) {
        Optional<Web2Secrets> oldWeb2Secrets = web2SecretsRepository.findWeb2SecretsByOwnerAddress(address);

        if (!oldWeb2Secrets.isPresent()) {
            Web2Secrets newWeb2Secrets = new Web2Secrets(address);
            return Optional.of(web2SecretsRepository.save(newWeb2Secrets));
        }

        if (shouldDecryptValues){
            for (Secret secret: oldWeb2Secrets.get().getSecrets()){
                secret.decryptValue(encryptionService);
            }
        }

        return oldWeb2Secrets;
    }

    public Optional<Secret> getSecret(String ownerAddress, String secretAddress, boolean shouldDecryptValue) {
        Optional<Web2Secrets> optionalWeb2Secrets = this.getWeb2Secrets(ownerAddress, shouldDecryptValue);

        if (!optionalWeb2Secrets.isPresent()) {
            log.error("Failed to getSecret (secret folder missing) [ownerAddress:{}, secretAddress:{}]", ownerAddress, secretAddress);
            return Optional.empty();
        }

        Web2Secrets web2Secrets = optionalWeb2Secrets.get();

        Secret secret = web2Secrets.getSecret(secretAddress);

        if (secret != null) {
            if (shouldDecryptValue){
                secret.decryptValue(encryptionService);
            }
            return Optional.of(secret);
        }

        return Optional.empty();
    }


    public boolean updateSecret(String ownerAddress, Secret newSecret) {
        newSecret.encryptValue(encryptionService);

        Optional<Web2Secrets> optionalSecretFolder = this.getWeb2Secrets(ownerAddress, false);

        if (!optionalSecretFolder.isPresent()) {
            log.error("Failed to updateSecret (secret folder missing) [ownerAddress:{}, secret:{}]", ownerAddress, newSecret);
            return false;
        }

        Web2Secrets web2Secrets = optionalSecretFolder.get();
        Secret existingSecret = web2Secrets.getSecret(newSecret.getAddress());

        if (existingSecret == null) {
            log.info("Adding newSecret [ownerAddress:{}, secretAddress:{}, secretValue:{}]",
                    ownerAddress, newSecret.getAddress(), newSecret.getValue());
            web2Secrets.getSecrets().add(newSecret);
            web2SecretsRepository.save(web2Secrets);
            return true;
        }

        if (!newSecret.getValue().equals(existingSecret.getValue())) {
            log.info("Updating secret [ownerAddress:{}, secretAddress:{}, oldSecretValueHash:{}, newSecretValueHsh:{}]",
                    ownerAddress, newSecret.getAddress(), existingSecret.getValue(), newSecret.getValue());
            existingSecret.setValue(newSecret.getValue(), true);
            web2SecretsRepository.save(web2Secrets);
            return true;
        }

        log.info("No need to update secret [ownerAddress:{}, secretAddress:{}, oldSecretValueHash:{}, newSecretValueHash:{}]",
                ownerAddress, newSecret.getAddress(), existingSecret.getValue(), newSecret.getValue());
        return true;
    }

}
