package com.iexec.sms.iexecsms.secret.iexec;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IexecSecretRepository extends MongoRepository<IexecSecret, String> {

    Optional<IexecSecret> findIexecSecretBySecretAddress(String secretAddress);

}
