package com.iexec.sms.iexecsms.secret;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class SecretFolderService {

    private SecretFolderRepository secretFolderRepository;

    public SecretFolderService(SecretFolderRepository secretFolderRepository) {
        this.secretFolderRepository = secretFolderRepository;
    }

    public Optional<SecretFolder> getSecretFolder(String address) {
        Optional<SecretFolder> oldFolder = secretFolderRepository.findSecretFolderByAddress(address);

        if (!oldFolder.isPresent()) {
            SecretFolder newSecretFolder = new SecretFolder(address);
            return Optional.of(secretFolderRepository.save(newSecretFolder));
        }

        return oldFolder;
    }

    public Optional<Secret> getSecret(String address, String secretAlias) {
        Optional<SecretFolder> optionalSecretFolder = this.getSecretFolder(address);

        if (!optionalSecretFolder.isPresent()) {
            log.error("Failed to getSecret (secret folder missing) [address:{}, alias:{}]", address, secretAlias);
            return Optional.empty();
        }

        SecretFolder secretFolder = optionalSecretFolder.get();

        Secret secret = secretFolder.getSecret(secretAlias);

        if (secret != null) {
            return Optional.of(secret);
        }

        return Optional.empty();
    }


    boolean updateSecret(String address, Secret newSecret) {
        Optional<SecretFolder> optionalSecretFolder = this.getSecretFolder(address);

        if (!optionalSecretFolder.isPresent()) {
            log.error("Failed to updateSecret (secret folder missing) [address:{}, secret:{}]", address, newSecret);
            return false;
        }

        SecretFolder secretFolder = optionalSecretFolder.get();
        Secret existingSecret = secretFolder.getSecret(newSecret.getAlias());

        if (existingSecret == null) {
            log.info("Adding newSecret [address:{}, secretAlias:{}, secretValue:{}]",
                    address, newSecret.getAlias(), newSecret.getValue());
            secretFolder.getSecrets().add(newSecret);
            secretFolderRepository.save(secretFolder);
            return true;
        }

        if (!newSecret.getValue().equals(existingSecret.getValue())) {
            log.info("Updating secret [address:{}, secretAlias:{},, oldSecretValue:{}, newSecretValue:{}]",
                    address, newSecret.getAlias(), existingSecret.getValue(), newSecret.getValue());
            existingSecret.setValue(newSecret.getValue());
            secretFolderRepository.save(secretFolder);
            return true;
        }

        log.info("No need to update secret [address:{}, secretAlias:{},, oldSecretValue:{}, newSecretValue:{}]",
                address, newSecret.getAlias(), existingSecret.getValue(), newSecret.getValue());
        return true;
    }

}
