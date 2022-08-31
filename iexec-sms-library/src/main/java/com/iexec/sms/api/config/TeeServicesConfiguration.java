package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.Getter;

@Getter
public abstract class TeeServicesConfiguration {

    private final TeeEnclaveProvider teeEnclaveProvider;
    private final TeeAppConfiguration preComputeConfiguration;
    private final TeeAppConfiguration postComputeConfiguration;

    protected TeeServicesConfiguration(TeeEnclaveProvider teeEnclaveProvider,
                                       TeeAppConfiguration preComputeConfiguration,
                                       TeeAppConfiguration postComputeConfiguration) {
        this.teeEnclaveProvider = teeEnclaveProvider;
        this.preComputeConfiguration = preComputeConfiguration;
        this.postComputeConfiguration = postComputeConfiguration;
    }
}
