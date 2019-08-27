package com.iexec.sms.iexecsms.secret.offchain;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OffChainSecretsRepository extends MongoRepository<OffChainSecrets, String> {

    Optional<OffChainSecrets> findOffChainSecretsByOwnerAddress(String ownerAddress);

}
