package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeFramework;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GramineServicesProperties extends TeeServicesProperties {

    public GramineServicesProperties() {
        super(TeeFramework.GRAMINE);
    }

    public GramineServicesProperties(TeeAppProperties preComputeProperties,
                                     TeeAppProperties postComputeProperties) {
        super(TeeFramework.GRAMINE, preComputeProperties, postComputeProperties);
    }
}
