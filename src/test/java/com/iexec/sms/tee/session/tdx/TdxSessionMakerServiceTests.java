/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.tdx;

import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.config.TdxServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.tdx.storage.TdxSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static com.iexec.sms.tee.session.tdx.TdxSessionMakerService.TDX_SESSION_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TdxSessionMakerServiceTests {
    private static final String TDX_FRAMEWORK_VERSION = "v1";

    @Mock
    private SecretSessionBaseService secretSessionBaseService;
    @InjectMocks
    private TdxSessionMakerService tdxSessionMakerService;

    private final TeeAppProperties preComputeProperties = TeeAppProperties.builder()
            .image("pre-compute-image")
            .fingerprint("pre-compute-fingerprint")
            .build();
    private final TeeAppProperties postComputeProperties = TeeAppProperties.builder()
            .image("post-compute-image")
            .fingerprint("post-compute-fingerprint")
            .build();
    private final SecretEnclaveBase preCompute = SecretEnclaveBase.builder()
            .name("pre-compute")
            .environment(Map.of("PRE_COMPUTE", "PRE_COMPUTE"))
            .build();
    private final SecretEnclaveBase appCompute = SecretEnclaveBase.builder()
            .name("app")
            .environment(Map.of("APP_COMPUTE", "APP_COMPUTE"))
            .build();
    private final SecretEnclaveBase postCompute = SecretEnclaveBase.builder()
            .name("post-compute")
            .environment(Map.of("POST_COMPUTE", "POST_COMPUTE"))
            .build();

    @Test
    void shouldGenerateTdxSessionWithPreCompute() throws TeeSessionGenerationException {
        final TeeEnclaveConfiguration enclaveConfig = TeeEnclaveConfiguration.builder()
                .version(TDX_FRAMEWORK_VERSION)
                .framework(TeeFramework.TDX)
                .fingerprint(APP_FINGERPRINT)
                .build();
        final TeeSessionRequest request = createSessionRequestBuilder(createTaskDescription(createDealParams().build(), enclaveConfig).build())
                .teeServicesProperties(new TdxServicesProperties(TDX_FRAMEWORK_VERSION, preComputeProperties, postComputeProperties))
                .build();

        final SecretSessionBase secretSessionBase = SecretSessionBase.builder()
                .preCompute(preCompute)
                .appCompute(appCompute)
                .postCompute(postCompute)
                .build();
        when(secretSessionBaseService.getSecretsTokens(request))
                .thenReturn(secretSessionBase);
        final TdxSession tdxSession = tdxSessionMakerService.generateSession(request);
        final List<TdxSession.Service> services = List.of(
                new TdxSession.Service(preCompute.getName(), "pre-compute-image", "pre-compute-fingerprint", Map.of("PRE_COMPUTE", "PRE_COMPUTE")),
                new TdxSession.Service(appCompute.getName(), APP_URI, APP_CHECKSUM, Map.of("APP_COMPUTE", "APP_COMPUTE")),
                new TdxSession.Service(postCompute.getName(), "post-compute-image", "post-compute-fingerprint", Map.of("POST_COMPUTE", "POST_COMPUTE"))
        );
        assertThat(tdxSession)
                .usingRecursiveComparison()
                .isEqualTo(new TdxSession(SESSION_ID, TDX_SESSION_VERSION, services));
    }

    @Test
    void shouldGenerateTdxSessionWithoutPreCompute() throws TeeSessionGenerationException {
        final TeeEnclaveConfiguration enclaveConfig = TeeEnclaveConfiguration.builder()
                .version(TDX_FRAMEWORK_VERSION)
                .framework(TeeFramework.TDX)
                .fingerprint(APP_FINGERPRINT)
                .build();
        final TeeSessionRequest request = createSessionRequestBuilder(createTaskDescription(createDealParams().build(), enclaveConfig).build())
                .teeServicesProperties(new TdxServicesProperties(TDX_FRAMEWORK_VERSION, preComputeProperties, postComputeProperties))
                .build();

        final SecretSessionBase secretSessionBase = SecretSessionBase.builder()
                .appCompute(appCompute)
                .postCompute(postCompute)
                .build();
        when(secretSessionBaseService.getSecretsTokens(request))
                .thenReturn(secretSessionBase);
        final TdxSession tdxSession = tdxSessionMakerService.generateSession(request);
        final List<TdxSession.Service> services = List.of(
                new TdxSession.Service(appCompute.getName(), APP_URI, APP_CHECKSUM, Map.of("APP_COMPUTE", "APP_COMPUTE")),
                new TdxSession.Service(postCompute.getName(), "post-compute-image", "post-compute-fingerprint", Map.of("POST_COMPUTE", "POST_COMPUTE"))
        );
        assertThat(tdxSession)
                .usingRecursiveComparison()
                .isEqualTo(new TdxSession(SESSION_ID, TDX_SESSION_VERSION, services));
    }
}
