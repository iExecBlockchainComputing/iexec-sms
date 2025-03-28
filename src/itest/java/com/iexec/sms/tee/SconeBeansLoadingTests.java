/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.sms.tee;

import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.tee.session.scone.SconeSessionHandlerService;
import com.iexec.sms.tee.session.scone.SconeSessionMakerService;
import com.iexec.sms.tee.session.scone.SconeSessionSecurityConfig;
import com.iexec.sms.tee.session.scone.cas.CasClient;
import com.iexec.sms.tee.session.scone.cas.CasConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles(profiles = {"scone", "test"})
public class SconeBeansLoadingTests extends TeeBeansLoadingTests {
    @Autowired
    Map<String, TeeServicesProperties> sconeServicesProperties;
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
