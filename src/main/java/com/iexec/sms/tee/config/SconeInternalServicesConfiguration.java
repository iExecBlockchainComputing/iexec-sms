package com.iexec.sms.tee.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.config.SconeServicesConfiguration;
import com.iexec.sms.api.config.TeeAppConfiguration;
import com.iexec.sms.tee.EnableIfTeeProvider;
import com.iexec.sms.tee.EnableIfTeeProviderDefinition;
import lombok.Getter;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(EnableIfTeeProvider.class)
@EnableIfTeeProviderDefinition(providers = TeeEnclaveProvider.SCONE)
@Getter
public class SconeInternalServicesConfiguration extends SconeServicesConfiguration {
    protected SconeInternalServicesConfiguration(
            TeeAppConfiguration preComputeConfiguration,
            TeeAppConfiguration postComputeConfiguration,
            String lasImage) {
        super(preComputeConfiguration, postComputeConfiguration, lasImage);
    }
}
