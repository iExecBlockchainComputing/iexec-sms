package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public abstract class TeeServicesConfiguration {
    private TeeEnclaveProvider teeEnclaveProvider;
    private TeeAppConfiguration preComputeConfiguration;
    private TeeAppConfiguration postComputeConfiguration;

    TeeServicesConfiguration(TeeEnclaveProvider teeEnclaveProvider) {
        this.teeEnclaveProvider = teeEnclaveProvider;
    }
}
