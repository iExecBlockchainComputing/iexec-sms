package com.iexec.sms.iexecsms.secret;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SecretRepository extends MongoRepository<Secret, String> {

    Optional<Secret> findSecretByAddress(String address);

}
