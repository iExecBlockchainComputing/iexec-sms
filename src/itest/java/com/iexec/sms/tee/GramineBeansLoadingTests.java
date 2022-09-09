package com.iexec.sms.tee;

import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.tee.session.gramine.GramineSessionHandlerService;
import com.iexec.sms.tee.session.gramine.GramineSessionMakerService;
import com.iexec.sms.tee.session.gramine.sps.SpsConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles(profiles = "gramine")
public class GramineBeansLoadingTests extends TeeBeansLoadingTests {
    @Autowired
    GramineServicesProperties gramineServicesProperties;
    @Autowired
    GramineSessionHandlerService gramineSessionHandlerService;
    @Autowired
    GramineSessionMakerService gramineSessionMakerService;
    @Autowired
    SpsConfiguration spsConfiguration;

    GramineBeansLoadingTests(@Autowired Environment environment) {
        super(environment);
    }

    @Test
    @Override
    void checkTeeBeansAreLoaded() {
        Assertions.assertNotNull(gramineServicesProperties);
        Assertions.assertNotNull(gramineSessionHandlerService);
        Assertions.assertNotNull(gramineSessionMakerService);
        Assertions.assertNotNull(spsConfiguration);
    }
}
