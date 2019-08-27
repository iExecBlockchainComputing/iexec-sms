package com.iexec.sms.iexecsms.secret.iexec;


import com.iexec.sms.iexecsms.secret.Secret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class IexecSecretService {

    private IexecSecretRepository iexecSecretRepository;

    public IexecSecretService(IexecSecretRepository iexecSecretRepository) {
        this.iexecSecretRepository = iexecSecretRepository;
    }

    public Optional<IexecSecret> getSecret(String secretAddress) {
        return iexecSecretRepository.findIexecSecretBySecretAddress(secretAddress);
    }

    public void updateSecret(String secretAddress, String newSecretValue) {
        Optional<IexecSecret> optionalExistingSecret = getSecret(secretAddress);

        if (!optionalExistingSecret.isPresent()) {
            log.info("Adding newSecret [secretAddress:{}, newSecretValue:{}]", secretAddress, newSecretValue);
            iexecSecretRepository.save(new IexecSecret(secretAddress, newSecretValue));
            return;
        }

        IexecSecret existingSecret = optionalExistingSecret.get();
        log.info("Updating secret [secretAddress:{}, oldSecretValue:{}, newSecretValue:{}]",
                secretAddress, existingSecret.getValue(), newSecretValue);
        existingSecret.setValue(newSecretValue);
        iexecSecretRepository.save(existingSecret);
    }

}
