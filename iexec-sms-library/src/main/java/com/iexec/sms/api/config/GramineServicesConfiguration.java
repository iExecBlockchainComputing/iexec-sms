package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeEnclaveProvider;

public class GramineServicesConfiguration extends TeeServicesConfiguration {

    public GramineServicesConfiguration(TeeAppConfiguration preComputeConfiguration,
                                           TeeAppConfiguration postComputeConfiguration) {
        super(TeeEnclaveProvider.GRAMINE, preComputeConfiguration, postComputeConfiguration);
    }
}
