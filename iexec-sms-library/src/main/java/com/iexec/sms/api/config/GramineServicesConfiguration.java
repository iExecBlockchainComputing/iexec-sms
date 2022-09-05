package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GramineServicesConfiguration extends TeeServicesConfiguration {

    public GramineServicesConfiguration() {
        super(TeeEnclaveProvider.GRAMINE);
    }

    public GramineServicesConfiguration(TeeAppConfiguration preComputeConfiguration,
                                        TeeAppConfiguration postComputeConfiguration) {
        super(TeeEnclaveProvider.GRAMINE, preComputeConfiguration, postComputeConfiguration);
    }
}
