package com.iexec.sms.iexecsms.secret.onchain;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class OnChainSecretService {

    private OnChainSecretRepository onChainSecretRepository;

    public OnChainSecretService(OnChainSecretRepository onChainSecretRepository) {
        this.onChainSecretRepository = onChainSecretRepository;
    }

    public Optional<OnChainSecret> getSecret(String secretAddress) {
        return onChainSecretRepository.findOnChainSecretByAddress(secretAddress);
    }

    public void updateSecret(String secretAddress, String newSecretValue) {
        Optional<OnChainSecret> optionalExistingSecret = getSecret(secretAddress);

        if (!optionalExistingSecret.isPresent()) {
            log.info("Adding newSecret [secretAddress:{}, newSecretValue:{}]", secretAddress, newSecretValue);
            onChainSecretRepository.save(new OnChainSecret(secretAddress, newSecretValue));
            return;
        }

        OnChainSecret existingSecret = optionalExistingSecret.get();
        log.info("Updating secret [secretAddress:{}, oldSecretValue:{}, newSecretValue:{}]",
                secretAddress, existingSecret.getValue(), newSecretValue);
        existingSecret.setValue(newSecretValue);
        onChainSecretRepository.save(existingSecret);
    }

}
