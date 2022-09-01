package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SconeServicesConfiguration extends TeeServicesConfiguration {
    private String lasImage;

    public SconeServicesConfiguration() {
        super(TeeEnclaveProvider.SCONE);
    }

    public SconeServicesConfiguration(TeeAppConfiguration preComputeConfiguration,
                                      TeeAppConfiguration postComputeConfiguration,
                                      String lasImage) {
        super(TeeEnclaveProvider.SCONE, preComputeConfiguration, postComputeConfiguration);
        this.lasImage = lasImage;
    }
}
