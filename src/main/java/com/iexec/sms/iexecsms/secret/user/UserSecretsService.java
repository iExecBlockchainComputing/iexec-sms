package com.iexec.sms.iexecsms.secret.user;


import com.iexec.sms.iexecsms.secret.Secret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class UserSecretsService {

    private UserSecretsRepository userSecretsRepository;

    public UserSecretsService(UserSecretsRepository userSecretsRepository) {
        this.userSecretsRepository = userSecretsRepository;
    }

    public Optional<UserSecrets> getUserSecrets(String address) {
        Optional<UserSecrets> oldFolder = userSecretsRepository.findUserSecretsByOwnerAddress(address);

        if (!oldFolder.isPresent()) {
            UserSecrets newUserSecrets = new UserSecrets(address);
            return Optional.of(userSecretsRepository.save(newUserSecrets));
        }

        return oldFolder;
    }

    public Optional<Secret> getSecret(String ownerAddress, String secretAddress) {
        Optional<UserSecrets> optionalSecretFolder = this.getUserSecrets(ownerAddress);

        if (!optionalSecretFolder.isPresent()) {
            log.error("Failed to getSecret (secret folder missing) [ownerAddress:{}, secretAddress:{}]", ownerAddress, secretAddress);
            return Optional.empty();
        }

        UserSecrets userSecrets = optionalSecretFolder.get();

        Secret secret = userSecrets.getSecret(secretAddress);

        if (secret != null) {
            return Optional.of(secret);
        }

        return Optional.empty();
    }


    public boolean updateSecret(String ownerAddress, Secret newSecret) {
        Optional<UserSecrets> optionalSecretFolder = this.getUserSecrets(ownerAddress);

        if (!optionalSecretFolder.isPresent()) {
            log.error("Failed to updateSecret (secret folder missing) [ownerAddress:{}, secret:{}]", ownerAddress, newSecret);
            return false;
        }

        UserSecrets userSecrets = optionalSecretFolder.get();
        Secret existingSecret = userSecrets.getSecret(newSecret.getAddress());

        if (existingSecret == null) {
            log.info("Adding newSecret [ownerAddress:{}, secretAddress:{}, secretValue:{}]",
                    ownerAddress, newSecret.getAddress(), newSecret.getValue());
            userSecrets.getSecrets().add(newSecret);
            userSecretsRepository.save(userSecrets);
            return true;
        }

        if (!newSecret.getValue().equals(existingSecret.getValue())) {
            log.info("Updating secret [ownerAddress:{}, secretAddress:{}, oldSecretValue:{}, newSecretValue:{}]",
                    ownerAddress, newSecret.getAddress(), existingSecret.getValue(), newSecret.getValue());
            existingSecret.setValue(newSecret.getValue());
            userSecretsRepository.save(userSecrets);
            return true;
        }

        log.info("No need to update secret [ownerAddress:{}, secretAddress:{},, oldSecretValue:{}, newSecretValue:{}]",
                ownerAddress, newSecret.getAddress(), existingSecret.getValue(), newSecret.getValue());
        return true;
    }

}
