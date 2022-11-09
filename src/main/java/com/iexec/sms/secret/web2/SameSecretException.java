package com.iexec.sms.secret.web2;

import lombok.Getter;

@Getter
public class SameSecretException extends Exception {
    private final String ownerAddress;
    private final String secretAddress;

    public SameSecretException(String ownerAddress, String secretAddress) {
        this.ownerAddress = ownerAddress;
        this.secretAddress = secretAddress;
    }
}
