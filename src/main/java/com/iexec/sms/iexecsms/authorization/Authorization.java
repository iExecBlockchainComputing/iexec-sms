package com.iexec.sms.iexecsms.authorization;

import com.iexec.common.security.Signature;
import com.iexec.common.utils.HashUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Authorization {

    private String chainTaskId;
    private String workerAddress;
    private String enclaveAddress;

    private Signature workerpoolSignature;
    private Signature workerSignature;

    public String getAuthorizationHash() {
        if (chainTaskId != null && workerAddress != null && enclaveAddress != null) {
            return HashUtils.concatenateAndHash(workerAddress, chainTaskId, enclaveAddress);
        }
        return "";
    }
}