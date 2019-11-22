package com.iexec.sms.iexecsms.secret.offchain;


import com.iexec.sms.iexecsms.secret.Secret;
import com.iexec.sms.iexecsms.encryption.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class OffChainSecretsService {

    private OffChainSecretsRepository offChainSecretsRepository;
    private EncryptionService encryptionService;

    public OffChainSecretsService(OffChainSecretsRepository offChainSecretsRepository,
                                  EncryptionService encryptionService) {
        this.offChainSecretsRepository = offChainSecretsRepository;
        this.encryptionService = encryptionService;
    }

    public Optional<OffChainSecrets> getOffChainSecrets(String address, boolean shouldDecryptValues) {
        Optional<OffChainSecrets> oldOffChainSecrets = offChainSecretsRepository.findOffChainSecretsByOwnerAddress(address);

        if (!oldOffChainSecrets.isPresent()) {
            OffChainSecrets newOffChainSecrets = new OffChainSecrets(address);
            return Optional.of(offChainSecretsRepository.save(newOffChainSecrets));
        }

        if (shouldDecryptValues){
            for (Secret secret: oldOffChainSecrets.get().getSecrets()){
                secret.decryptValue(encryptionService);
            }
        }

        return oldOffChainSecrets;
    }

    public Optional<Secret> getSecret(String ownerAddress, String secretAddress, boolean shouldDecryptValue) {
        Optional<OffChainSecrets> optionalOffChainSecrets = this.getOffChainSecrets(ownerAddress, shouldDecryptValue);

        if (!optionalOffChainSecrets.isPresent()) {
            log.error("Failed to getSecret (secret folder missing) [ownerAddress:{}, secretAddress:{}]", ownerAddress, secretAddress);
            return Optional.empty();
        }

        OffChainSecrets offChainSecrets = optionalOffChainSecrets.get();

        Secret secret = offChainSecrets.getSecret(secretAddress);

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

        Optional<OffChainSecrets> optionalSecretFolder = this.getOffChainSecrets(ownerAddress, false);

        if (!optionalSecretFolder.isPresent()) {
            log.error("Failed to updateSecret (secret folder missing) [ownerAddress:{}, secret:{}]", ownerAddress, newSecret);
            return false;
        }

        OffChainSecrets offChainSecrets = optionalSecretFolder.get();
        Secret existingSecret = offChainSecrets.getSecret(newSecret.getAddress());

        if (existingSecret == null) {
            log.info("Adding newSecret [ownerAddress:{}, secretAddress:{}, secretValue:{}]",
                    ownerAddress, newSecret.getAddress(), newSecret.getValue());
            offChainSecrets.getSecrets().add(newSecret);
            offChainSecretsRepository.save(offChainSecrets);
            return true;
        }

        if (!newSecret.getValue().equals(existingSecret.getValue())) {
            log.info("Updating secret [ownerAddress:{}, secretAddress:{}, oldSecretValueHash:{}, newSecretValueHsh:{}]",
                    ownerAddress, newSecret.getAddress(), existingSecret.getValue(), newSecret.getValue());
            existingSecret.setValue(newSecret.getValue(), true);
            offChainSecretsRepository.save(offChainSecrets);
            return true;
        }

        log.info("No need to update secret [ownerAddress:{}, secretAddress:{}, oldSecretValueHash:{}, newSecretValueHash:{}]",
                ownerAddress, newSecret.getAddress(), existingSecret.getValue(), newSecret.getValue());
        return true;
    }

}
