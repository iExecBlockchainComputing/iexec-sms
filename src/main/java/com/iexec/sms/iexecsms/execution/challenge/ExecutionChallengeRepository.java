package com.iexec.sms.iexecsms.execution.challenge;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ExecutionChallengeRepository extends MongoRepository<ExecutionAttestor, String> {
    Optional<ExecutionAttestor> findByTaskId(String taskId);
}
