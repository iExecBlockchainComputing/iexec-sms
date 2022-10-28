package com.iexec.sms.secret.web2;

import com.iexec.sms.secret.Secret;
import lombok.Getter;

@Getter
public class SameSecretException extends Exception {
    private final String ownerAddress;
    private final String secretAddress;

    public SameSecretException(String ownerAddress, Secret secret) {
        this.ownerAddress = ownerAddress;
        this.secretAddress = secret.getAddress();
    }
}
