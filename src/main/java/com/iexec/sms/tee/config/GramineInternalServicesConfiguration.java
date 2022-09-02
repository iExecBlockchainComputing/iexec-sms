package com.iexec.sms.tee.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.config.GramineServicesConfiguration;
import com.iexec.sms.api.config.TeeAppConfiguration;
import com.iexec.sms.api.config.TeeServicesConfiguration;
import com.iexec.sms.tee.EnableIfTeeProvider;
import com.iexec.sms.tee.EnableIfTeeProviderDefinition;
import lombok.Getter;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(EnableIfTeeProvider.class)
@EnableIfTeeProviderDefinition(providers = TeeEnclaveProvider.GRAMINE)
@Getter
public class GramineInternalServicesConfiguration
        extends GramineServicesConfiguration
        implements TeeInternalServicesConfiguration {
    public GramineInternalServicesConfiguration(
            TeeAppConfiguration preComputeConfiguration,
            TeeAppConfiguration postComputeConfiguration) {
        super(preComputeConfiguration, postComputeConfiguration);
    }

    @Override
    public TeeServicesConfiguration getShareableConfiguration() {
        return new GramineServicesConfiguration(getPreComputeConfiguration(), getPostComputeConfiguration());
    }
}
