package com.iexec.sms.tee.challenge;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TeeChallengeRepository extends CrudRepository<TeeChallenge, String> {
    Optional<TeeChallenge> findByTaskId(String taskId);
}
