package com.iexec.sms.iexecsms.secret.onchain;


import com.iexec.sms.iexecsms.encryption.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class OnChainSecretService {

    private OnChainSecretRepository onChainSecretRepository;
    private EncryptionService encryptionService;

    public OnChainSecretService(OnChainSecretRepository onChainSecretRepository,
                                EncryptionService encryptionService) {
        this.onChainSecretRepository = onChainSecretRepository;
        this.encryptionService = encryptionService;
    }

    public Optional<OnChainSecret> getSecret(String secretAddress, boolean shouldDecryptValue) {
        Optional<OnChainSecret> optionalSecret = onChainSecretRepository.findOnChainSecretByAddress(secretAddress);

        if (!optionalSecret.isEmpty()) {
            OnChainSecret secret = optionalSecret.get();
            if (shouldDecryptValue) {
                secret.decryptValue(encryptionService);
            }
            return Optional.of(secret);
        }

        return Optional.empty();

    }

    public Optional<OnChainSecret> getSecret(String secretAddress) {
        return getSecret(secretAddress, false);

    }

    /*
     *
     * Stores encrypted secrets
     * */
    public void updateSecret(String secretAddress, String unencryptedSecretValue) {
        OnChainSecret onChainSecret = new OnChainSecret(secretAddress, unencryptedSecretValue);
        onChainSecret.encryptValue(encryptionService);

        Optional<OnChainSecret> optionalExistingSecret = getSecret(secretAddress);

        if (!optionalExistingSecret.isPresent()) {
            onChainSecretRepository.save(onChainSecret);
            log.info("Added newSecret [secretAddress:{}, secretValueHash:{}]",
                    secretAddress, onChainSecret.getValue());
            return;
        }

        OnChainSecret existingSecret = optionalExistingSecret.get();
        existingSecret.setValue(onChainSecret.getValue(), true);

        onChainSecretRepository.save(existingSecret);

        log.info("Updated secret [secretAddress:{}, secretValueHash:{}]",
                secretAddress, existingSecret.getValue());
    }

}
