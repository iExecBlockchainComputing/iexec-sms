package com.iexec.sms.iexecsms.tee.session.fingerprint;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DatasetFingerprint {

    String fspfKey;
    String fspfTag;

}