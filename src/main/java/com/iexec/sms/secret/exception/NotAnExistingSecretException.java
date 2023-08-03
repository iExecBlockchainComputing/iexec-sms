package com.iexec.sms.secret.exception;

import com.iexec.sms.secret.base.AbstractSecretHeader;
import lombok.Getter;

@Getter
public class NotAnExistingSecretException extends Exception {
    private final AbstractSecretHeader header;

    public NotAnExistingSecretException(AbstractSecretHeader header) {
        this.header = header;
    }
}
