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

    public GramineServicesProperties(TeeAppProperties preComputeConfiguration,
                                     TeeAppProperties postComputeConfiguration) {
        super(TeeEnclaveProvider.GRAMINE, preComputeConfiguration, postComputeConfiguration);
    }
}
