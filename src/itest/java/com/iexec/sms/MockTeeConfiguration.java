package com.iexec.sms;

import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.tee.config.TeeInternalServicesConfiguration;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.iexec.sms.MockTeeConfiguration.MOCK_TEE_PROFILE;

@Configuration
@Profile(MOCK_TEE_PROFILE)
public class MockTeeConfiguration {
    public static final String MOCK_TEE_PROFILE = "mock-tee";

    @MockBean
    private TeeSessionHandler teeSessionHandler;
    @MockBean
    private TeeInternalServicesConfiguration teeInternalServicesConfiguration;
    @MockBean
    private TeeServicesProperties teeServicesProperties;
    @MockBean
    @Qualifier("preComputeConfiguration")
    private TeeAppProperties preComputeConfiguration;
    @MockBean
    @Qualifier("postComputeConfiguration")
    private TeeAppProperties postComputeConfiguration;
}
