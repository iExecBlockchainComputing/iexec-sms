package com.iexec.sms.iexecsms.tee.challenge;

import com.iexec.sms.iexecsms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;

import java.util.Optional;

@Slf4j
@Service
public class TeeChallengeService {

    private TeeChallengeRepository teeChallengeRepository;

    public TeeChallengeService(TeeChallengeRepository teeChallengeRepository) {
        this.teeChallengeRepository = teeChallengeRepository;
    }

    public Optional<TeeChallenge> getOrCreate(String taskId) {
        // if existing returns from the db
        Optional<TeeChallenge> optionalTeeChallenge = teeChallengeRepository.findByTaskId(taskId);
        if (optionalTeeChallenge.isPresent()) {
            return optionalTeeChallenge;
        }

        // otherwise create it
        try {
            TeeChallenge teeChallenge = teeChallengeRepository.save(TeeChallenge.builder()
                    .credentials(new EthereumCredentials(Keys.createEcKeyPair()))
                    .taskId(taskId)
                    .build());
            log.info("Created tee challenge [chainTaskId:{}, teeChallenge:{}]",
                    taskId, teeChallenge.getCredentials().getAddress());
            return Optional.of(teeChallenge);
        } catch (Exception e) {
            log.error("Couldn't create credentials [exception:{}]", e.getMessage());
            return Optional.empty();
        }
    }
}
