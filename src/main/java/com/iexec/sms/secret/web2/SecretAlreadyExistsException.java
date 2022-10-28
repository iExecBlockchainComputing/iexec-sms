package com.iexec.sms.secret.web2;

import lombok.Getter;

@Getter
public class SecretAlreadyExistsException extends Exception {
    private final String ownerAddress;
    private final String secretAddress;

    public SecretAlreadyExistsException(String ownerAddress, String secretAddress) {
        this.ownerAddress = ownerAddress;
        this.secretAddress = secretAddress;
    }
}
