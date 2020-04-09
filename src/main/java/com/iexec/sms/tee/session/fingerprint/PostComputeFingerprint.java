package com.iexec.sms.tee.session.fingerprint;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostComputeFingerprint {

    private String fspfKey;
    private String fspfTag;
    private String mrEnclave;

}