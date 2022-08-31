package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.Getter;

@Getter
public class SconeServicesConfiguration extends TeeServicesConfiguration {
    private final String lasImage;

    public SconeServicesConfiguration(
            TeeAppConfiguration preComputeConfiguration,
            TeeAppConfiguration postComputeConfiguration,
            String lasImage) {
        super(TeeEnclaveProvider.SCONE, preComputeConfiguration, postComputeConfiguration);
        this.lasImage = lasImage;
    }
}
