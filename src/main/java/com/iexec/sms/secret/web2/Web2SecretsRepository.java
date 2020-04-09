package com.iexec.sms.secret.web2;


import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface Web2SecretsRepository extends CrudRepository<Web2Secrets, String> {

    Optional<Web2Secrets> findWeb2SecretsByOwnerAddress(String ownerAddress);

}
