package com.iexec.sms.api.config;

import com.iexec.common.tee.TeeFramework;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public abstract class TeeServicesProperties {
    private TeeFramework teeFramework;
    private TeeAppProperties preComputeProperties;
    private TeeAppProperties postComputeProperties;

    TeeServicesProperties(TeeFramework teeFramework) {
        this.teeFramework = teeFramework;
    }
}
