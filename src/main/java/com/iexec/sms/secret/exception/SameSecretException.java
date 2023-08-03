package com.iexec.sms.secret.exception;

import com.iexec.sms.secret.base.AbstractSecretHeader;
import lombok.Getter;

@Getter
public class SameSecretException extends Exception {
    private final AbstractSecretHeader header;

    public SameSecretException(AbstractSecretHeader header) {
        this.header = header;
    }
}
