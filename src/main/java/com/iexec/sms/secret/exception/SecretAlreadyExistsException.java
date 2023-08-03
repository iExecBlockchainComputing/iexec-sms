package com.iexec.sms.secret.exception;

import com.iexec.sms.secret.base.AbstractSecretHeader;
import lombok.Getter;

@Getter
public class SecretAlreadyExistsException extends Exception {
    private final AbstractSecretHeader header;

    public SecretAlreadyExistsException(AbstractSecretHeader header) {
        this.header = header;
    }
}
