package com.iexec.sms.iexecsms.secret.user;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserSecretsRepository extends MongoRepository<UserSecrets, String> {

    Optional<UserSecrets> findUserSecretsByOwnerAddress(String ownerAddress);

}
