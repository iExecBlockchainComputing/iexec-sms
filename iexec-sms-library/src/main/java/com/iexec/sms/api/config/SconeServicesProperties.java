package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SconeServicesProperties extends TeeServicesProperties {
    private String lasImage;

    public SconeServicesProperties() {
        super(TeeEnclaveProvider.SCONE);
    }

    public SconeServicesProperties(TeeAppProperties preComputeProperties,
                                   TeeAppProperties postComputeProperties,
                                   String lasImage) {
        super(TeeEnclaveProvider.SCONE, preComputeProperties, postComputeProperties);
        this.lasImage = lasImage;
    }
}
