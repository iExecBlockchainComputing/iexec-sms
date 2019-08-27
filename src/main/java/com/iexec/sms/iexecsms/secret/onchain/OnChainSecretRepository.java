package com.iexec.sms.iexecsms.secret.onchain;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OnChainSecretRepository extends MongoRepository<OnChainSecret, String> {

    Optional<OnChainSecret> findOnChainSecretByAddress(String secretAddress);

}
