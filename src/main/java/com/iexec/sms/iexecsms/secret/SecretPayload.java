package com.iexec.sms.iexecsms.secret;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@NoArgsConstructor
public class SecretPayload {

    private String symmetricKey; //(Kd, Kb, Ke)
    private String beneficiaryCredentials; //dropbox, AWS, TODO think about

    SecretPayload updateSecretPayloadFields(SecretPayload newSecretPayload) {
        if (!newSecretPayload.getSymmetricKey().isEmpty()) {
            this.setSymmetricKey(newSecretPayload.getSymmetricKey());
        }
        if (!newSecretPayload.getBeneficiaryCredentials().isEmpty()) {
            this.setBeneficiaryCredentials(newSecretPayload.getBeneficiaryCredentials());
        }
        return this;
    }

}
