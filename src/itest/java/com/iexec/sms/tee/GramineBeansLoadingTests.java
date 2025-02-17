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
import com.iexec.sms.tee.session.gramine.GramineSessionHandlerService;
import com.iexec.sms.tee.session.gramine.GramineSessionMakerService;
import com.iexec.sms.tee.session.gramine.sps.SpsConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@ActiveProfiles(profiles = {"gramine", "test"})
public class GramineBeansLoadingTests extends TeeBeansLoadingTests {
    @Autowired
    Map<String, TeeServicesProperties> gramineServicesProperties;
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
