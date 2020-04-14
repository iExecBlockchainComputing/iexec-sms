package com.iexec.sms.tee.challenge;

import java.util.Optional;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.utils.EthereumCredentials;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TeeChallengeService {

    private TeeChallengeRepository teeChallengeRepository;
    private EncryptionService encryptionService;

    public TeeChallengeService(TeeChallengeRepository teeChallengeRepository,
                               EncryptionService encryptionService) {
        this.teeChallengeRepository = teeChallengeRepository;
        this.encryptionService = encryptionService;
    }

    public Optional<TeeChallenge> getOrCreate(String taskId, boolean shouldDecryptKeys) {
        // if existing returns from the db
        Optional<TeeChallenge> optionalTeeChallenge = teeChallengeRepository.findByTaskId(taskId);
        if (optionalTeeChallenge.isPresent()) {
            if (shouldDecryptKeys) { //eventually decrypt if wanted
                decryptChallengeKeys(optionalTeeChallenge.get());
            }
            return optionalTeeChallenge;
        }

        // otherwise create it
        try {
            TeeChallenge teeChallenge = new TeeChallenge(taskId);
            encryptChallengeKeys(teeChallenge);
            teeChallenge = teeChallengeRepository.save(teeChallenge);
            log.info("Created tee challenge [chainTaskId:{}, teeChallenge:{}]",
                    taskId, teeChallenge.getCredentials().getAddress());

            if (shouldDecryptKeys) { //eventually decrypt if wanted
                decryptChallengeKeys(teeChallenge);
            }

            return Optional.of(teeChallenge);
        } catch (Exception e) {
            log.error("Couldn't create credentials [exception:{}]", e.getMessage());
            return Optional.empty();
        }
    }

    public void encryptChallengeKeys(TeeChallenge teeChallenge) {
        EthereumCredentials credentials = teeChallenge.getCredentials();
        if (!credentials.isEncrypted()) {
            String encPrivateKey = encryptionService.encrypt(credentials.getPrivateKey());
            String encPublicKey = encryptionService.encrypt(credentials.getPublicKey());
            credentials.setEncryptedKeys(encPrivateKey, encPublicKey);
        }
    }

    public void decryptChallengeKeys(TeeChallenge teeChallenge) {
        EthereumCredentials credentials = teeChallenge.getCredentials();
        if (credentials.isEncrypted()) {
            String privateKey = encryptionService.decrypt(credentials.getPrivateKey());
            String publicKey = encryptionService.decrypt(credentials.getPublicKey());
            credentials.setPlainKeys(privateKey, publicKey);
        }
    }
}
