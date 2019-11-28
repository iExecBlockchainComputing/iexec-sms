package com.iexec.sms.iexecsms.secret.onchain;


import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface OnChainSecretRepository extends CrudRepository<OnChainSecret, String> {

    Optional<OnChainSecret> findOnChainSecretByAddress(String secretAddress);

}
