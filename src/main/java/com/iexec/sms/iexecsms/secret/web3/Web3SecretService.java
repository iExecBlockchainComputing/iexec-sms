package com.iexec.sms.iexecsms.secret.web3;


import com.iexec.sms.iexecsms.encryption.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class Web3SecretService {

    private Web3SecretRepository web3SecretRepository;
    private EncryptionService encryptionService;

    public Web3SecretService(Web3SecretRepository web3SecretRepository,
                             EncryptionService encryptionService) {
        this.web3SecretRepository = web3SecretRepository;
        this.encryptionService = encryptionService;
    }

    public Optional<Web3Secret> getSecret(String secretAddress, boolean shouldDecryptValue) {
        Optional<Web3Secret> optionalSecret = web3SecretRepository.findWeb3SecretByAddress(secretAddress);

        if (!optionalSecret.isEmpty()) {
            Web3Secret secret = optionalSecret.get();
            if (shouldDecryptValue) {
                secret.decryptValue(encryptionService);
            }
            return Optional.of(secret);
        }

        return Optional.empty();

    }

    public Optional<Web3Secret> getSecret(String secretAddress) {
        return getSecret(secretAddress, false);

    }

    /*
     *
     * Stores encrypted secrets
     * */
    public void updateSecret(String secretAddress, String unencryptedSecretValue) {
        Web3Secret web3Secret = new Web3Secret(secretAddress, unencryptedSecretValue);
        web3Secret.encryptValue(encryptionService);

        Optional<Web3Secret> optionalExistingSecret = getSecret(secretAddress);

        if (!optionalExistingSecret.isPresent()) {
            web3SecretRepository.save(web3Secret);
            log.info("Added newSecret [secretAddress:{}, secretValueHash:{}]",
                    secretAddress, web3Secret.getValue());
            return;
        }

        Web3Secret existingSecret = optionalExistingSecret.get();
        existingSecret.setValue(web3Secret.getValue(), true);

        web3SecretRepository.save(existingSecret);

        log.info("Updated secret [secretAddress:{}, secretValueHash:{}]",
                secretAddress, existingSecret.getValue());
    }

}
