package com.iexec.sms.tee.challenge;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;

import java.util.Optional;

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
                optionalTeeChallenge.get().getCredentials().decryptKeys(encryptionService);
            }
            return optionalTeeChallenge;
        }

        // otherwise create it
        try {
            EthereumCredentials ethereumCredentials = new EthereumCredentials(Keys.createEcKeyPair());
            ethereumCredentials.encryptKeys(encryptionService);
            //store encrypted credentials of challenge
            TeeChallenge teeChallenge = teeChallengeRepository.save(TeeChallenge.builder()
                    .credentials(ethereumCredentials)
                    .taskId(taskId)
                    .build());
            log.info("Created tee challenge [chainTaskId:{}, teeChallenge:{}]",
                    taskId, teeChallenge.getCredentials().getAddress());

            if (shouldDecryptKeys) { //eventually decrypt if wanted
                teeChallenge.getCredentials().decryptKeys(encryptionService);
            }

            return Optional.of(teeChallenge);
        } catch (Exception e) {
            log.error("Couldn't create credentials [exception:{}]", e.getMessage());
            return Optional.empty();
        }
    }
}
