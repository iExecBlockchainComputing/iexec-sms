package com.iexec.sms.iexecsms.challenge;

import com.iexec.sms.iexecsms.credential.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;

import java.util.Optional;

@Slf4j
@Service
public class ExecutionChallengeService {

    private ExecutionChallengeRepository executionChallengeRepository;

    public ExecutionChallengeService(ExecutionChallengeRepository executionChallengeRepository) {
        this.executionChallengeRepository = executionChallengeRepository;
    }

    public Optional<ExecutionAttestor> getOrCreate(String taskId) {
        // if existing returns from the db
        Optional<ExecutionAttestor> oAttestor = executionChallengeRepository.findByTaskId(taskId);
        if (oAttestor.isPresent()) {
            return oAttestor;
        }

        // otherwise create it
        try {
            ExecutionAttestor attestor = executionChallengeRepository.save(ExecutionAttestor.builder()
                    .credentials(new EthereumCredentials(Keys.createEcKeyPair()))
                    .taskId(taskId)
                    .build());
            log.info("Created execution challenge [chainTaskId:{}, enclaveChallenge:{}]",
                    taskId, attestor.getCredentials().getAddress());
            return Optional.of(attestor);
        } catch (Exception e) {
            log.error("Couldn't create credentials [exception:{}]", e.getMessage());
            return Optional.empty();
        }
    }
}
