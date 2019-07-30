package com.iexec.sms.iexecsms.secret;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class SecretService {

    private SecretRepository secretRepository;

    public SecretService(SecretRepository secretRepository) {
        this.secretRepository = secretRepository;
    }

    Optional<Secret> getSecret(String owner) {
        return secretRepository.findSecretByOwner(owner);
    }

    /*
     * Remove secret logs in prod (*)
     * */
    boolean setSecret(Secret newSecret) {
        if (newSecret == null || newSecret.getOwner() == null || newSecret.getPayload() == null) {
            return false;
        }
        String owner = newSecret.getOwner();
        SecretPayload newSecretPayload = newSecret.getPayload();

        Optional<Secret> optionalExistingSecret = secretRepository.findSecretByOwner(owner);
        Secret secretToSave;
        if (optionalExistingSecret.isPresent()) {
            Secret existingSecret = optionalExistingSecret.get();
            SecretPayload existingSecretPayload = existingSecret.getPayload();
            log.info("Secret update [owner:{}, newSecretPayload:{}]", owner, newSecretPayload);// (*)
            existingSecret.setPayload(existingSecretPayload.updateSecretPayloadFields(newSecretPayload));
            secretToSave = existingSecret;
        } else {
            log.info("New secret [owner:{}, newSecretPayload:{}]", owner, newSecretPayload);// (*)
            secretToSave = Secret.builder().owner(owner).payload(newSecretPayload).build();
        }

        return secretRepository.save(secretToSave) != null;
    }


}
