package com.iexec.sms.iexecsms.secret.offchain;


import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface OffChainSecretsRepository extends CrudRepository<OffChainSecrets, String> {

    Optional<OffChainSecrets> findOffChainSecretsByOwnerAddress(String ownerAddress);

}
