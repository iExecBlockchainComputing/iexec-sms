package com.iexec.sms.iexecsms.secret;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SecretFolderRepository extends MongoRepository<SecretFolder, String> {

    Optional<SecretFolder> findSecretFolderByAddress(String address);

}
