package com.iexec.sms.iexecsms.tee.challenge;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TeeChallengeRepository extends MongoRepository<TeeChallenge, String> {
    Optional<TeeChallenge> findByTaskId(String taskId);
}
