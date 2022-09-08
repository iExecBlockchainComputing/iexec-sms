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
public abstract class TeeServicesProperties {
    private TeeEnclaveProvider teeEnclaveProvider;
    private TeeAppProperties preComputeProperties;
    private TeeAppProperties postComputeProperties;

    TeeServicesProperties(TeeEnclaveProvider teeEnclaveProvider) {
        this.teeEnclaveProvider = teeEnclaveProvider;
    }

    public abstract TeeServicesProperties getProperties();
}
