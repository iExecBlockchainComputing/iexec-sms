package com.iexec.sms.iexecsms.attestation;

import lombok.*;
import org.web3j.crypto.Credentials;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attestation {

    private String taskId;
    private Credentials credentials;
}
