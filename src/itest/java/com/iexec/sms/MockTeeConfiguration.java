package com.iexec.sms;

import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
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
    private TeeServicesProperties teeServicesProperties;
}
