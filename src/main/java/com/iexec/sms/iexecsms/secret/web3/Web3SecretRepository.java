package com.iexec.sms.iexecsms.secret.web3;


import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface Web3SecretRepository extends CrudRepository<Web3Secret, String> {

    Optional<Web3Secret> findWeb3SecretByAddress(String secretAddress);

}
