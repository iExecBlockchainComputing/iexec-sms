package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeFramework;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SconeServicesProperties extends TeeServicesProperties {
    private String lasImage;

    public SconeServicesProperties() {
        super(TeeFramework.SCONE);
    }

    public SconeServicesProperties(TeeAppProperties preComputeProperties,
                                   TeeAppProperties postComputeProperties,
                                   String lasImage) {
        super(TeeFramework.SCONE, preComputeProperties, postComputeProperties);
        this.lasImage = lasImage;
    }
}
