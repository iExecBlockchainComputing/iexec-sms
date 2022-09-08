package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GramineServicesProperties extends TeeServicesProperties {

    public GramineServicesProperties() {
        super(TeeEnclaveProvider.GRAMINE);
    }

    public GramineServicesProperties(TeeAppProperties preComputeProperties,
                                     TeeAppProperties postComputeProperties) {
        super(TeeEnclaveProvider.GRAMINE, preComputeProperties, postComputeProperties);
    }
}
