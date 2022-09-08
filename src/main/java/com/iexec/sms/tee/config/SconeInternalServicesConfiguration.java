package com.iexec.sms.tee.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.tee.ConditionalOnTeeProvider;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotBlank;

@Configuration
@ConditionalOnTeeProvider(providers = TeeEnclaveProvider.SCONE)
@Getter
public class SconeInternalServicesConfiguration
        extends SconeServicesProperties {
    public SconeInternalServicesConfiguration(
            TeeAppProperties preComputeProperties,
            TeeAppProperties postComputeProperties,
            @Value("${tee.scone.las-image}")
            @NotBlank(message = "las image must be provided")
            String lasImage) {
        super(preComputeProperties, postComputeProperties, lasImage);
    }

    @Override
    public TeeServicesProperties getProperties() {
        return new SconeServicesProperties(getPreComputeProperties(), getPostComputeProperties(), getLasImage());
    }
}
