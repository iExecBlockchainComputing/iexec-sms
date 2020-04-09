package com.iexec.sms.tee.session.fingerprint;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DatasetFingerprint {

    String fspfKey;
    String fspfTag;

}