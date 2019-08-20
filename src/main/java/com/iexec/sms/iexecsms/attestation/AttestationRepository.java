package com.iexec.sms.iexecsms.attestation;

import com.iexec.common.security.Attestation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AttestationRepository extends MongoRepository<Attestation, String> {
    Optional<Attestation> findByTaskId(String taskId);
}
