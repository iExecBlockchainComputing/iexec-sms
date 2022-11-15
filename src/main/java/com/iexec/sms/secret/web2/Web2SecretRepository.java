package com.iexec.sms.secret.web2;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface Web2SecretRepository extends CrudRepository<Web2Secret, String> {
    Optional<Web2Secret> findByOwnerAddressAndAddressAllIgnoreCase(String ownerAddress, String address);
}
