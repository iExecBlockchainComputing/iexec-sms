package com.iexec.sms.iexecsms.secret;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@NoArgsConstructor
public class SecretPayload {

    public String symmetricKey;
    public String credentials;
    // ...


    SecretPayload updateSecretPayloadFields(SecretPayload newSecretPayload) {
        if (!newSecretPayload.getSymmetricKey().isEmpty()) {
            this.setSymmetricKey(newSecretPayload.getSymmetricKey());
        }
        if (!newSecretPayload.getCredentials().isEmpty()) {
            this.setCredentials(newSecretPayload.getCredentials());
        }
        return this;
    }

}
