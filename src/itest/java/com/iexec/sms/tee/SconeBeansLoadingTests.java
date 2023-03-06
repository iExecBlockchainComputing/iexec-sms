package com.iexec.sms.tee;

import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.tee.session.scone.SconeSessionHandlerService;
import com.iexec.sms.tee.session.scone.SconeSessionMakerService;
import com.iexec.sms.tee.session.scone.SconeSessionSecurityConfig;
import com.iexec.sms.tee.session.scone.cas.CasClient;
import com.iexec.sms.tee.session.scone.cas.CasConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles(profiles = {"scone", "test"})
public class SconeBeansLoadingTests extends TeeBeansLoadingTests {
    @Autowired
    SconeServicesProperties sconeServicesProperties;
    @Autowired
    SconeSessionHandlerService sconeSessionHandlerService;
    @Autowired
    SconeSessionMakerService sconeSessionMakerService;
    @Autowired
    SconeSessionSecurityConfig sconeSessionSecurityConfig;
    @Autowired
    CasClient casClient;
    @Autowired
    CasConfiguration casConfiguration;

    SconeBeansLoadingTests(@Autowired Environment environment) {
        super(environment);
    }

    @Test
    @Override
    void checkTeeBeansAreLoaded() {
        assertNotNull(sconeServicesProperties);
        assertNotNull(sconeSessionHandlerService);
        assertNotNull(sconeSessionMakerService);
        assertNotNull(sconeSessionSecurityConfig);
        assertNotNull(casClient);
        assertNotNull(casConfiguration);
    }
}
