package com.iexec.sms.secret.web2;

import lombok.Getter;

@Getter
public class NotAnExistingSecretException extends Exception {
    private final String ownerAddress;
    private final String secretAddress;

    public NotAnExistingSecretException(String ownerAddress, String secretAddress) {
        this.ownerAddress = ownerAddress;
        this.secretAddress = secretAddress;
    }
}
