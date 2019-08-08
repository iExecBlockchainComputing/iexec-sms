package com.iexec.sms.iexecsms.attestation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;

import java.util.Optional;

@Slf4j
@Service
public class AttestationService {

    private AttestationRepository attestationRepository;

    public AttestationService(AttestationRepository attestationRepository) {
        this.attestationRepository = attestationRepository;
    }

    Optional<Attestation> getOrCreate(String taskId) {
        // if existing returns from the db
        Optional<Attestation> oAttestation = attestationRepository.findByTaskId(taskId);
        if(oAttestation.isPresent()) {
            return oAttestation;
        }

        // otherwise create it
        try {
            Credentials credentials = Credentials.create(Keys.createEcKeyPair());
            return Optional.of(Attestation.builder()
                    .credentials(credentials)
                    .taskId(taskId)
                    .build());
        } catch (Exception e) {
            log.error("Couldn't create credentials [exception:{}]", e.getMessage());
            return Optional.empty();
        }
    }
}
