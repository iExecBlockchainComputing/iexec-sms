/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.scone.palaemon;

import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.utils.FileHelper;
import com.iexec.sms.tee.session.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.generic.TeeSecretsService;
import com.iexec.sms.tee.session.scone.attestation.AttestationSecurityConfig;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static org.mockito.Mockito.*;

@Slf4j
class PalaemonSessionServiceTests {

    private static final String TEMPLATE_SESSION_FILE = "src/main/resources/palaemonTemplate.vm";
    private static final String EXPECTED_SESSION_FILE = "src/test/resources/palaemon-tee-session.yml";
    private static final String PRE_COMPUTE_ENTRYPOINT = "entrypoint1";
    private static final String APP_FINGERPRINT = "01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b";
    private static final String APP_ENTRYPOINT = "appEntrypoint";
    private static final TeeEnclaveConfiguration enclaveConfig =
            mock(TeeEnclaveConfiguration.class);
    private static final String POST_COMPUTE_ENTRYPOINT = "entrypoint3";

    @Mock
    private TeeWorkflowConfiguration teeWorkflowConfig;

    @Spy
    @InjectMocks
    private TeeSecretsService teeSecretsService;
    @Mock
    private AttestationSecurityConfig attestationSecurityConfig;

    private PalaemonSessionService palaemonSessionService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        palaemonSessionService = spy(new PalaemonSessionService(teeSecretsService, teeWorkflowConfig, attestationSecurityConfig));
        ReflectionTestUtils.setField(palaemonSessionService, "palaemonTemplateFilePath", TEMPLATE_SESSION_FILE);

        when(enclaveConfig.getFingerprint()).thenReturn(APP_FINGERPRINT);
        when(enclaveConfig.getEntrypoint()).thenReturn(APP_ENTRYPOINT);
    }

    //region getSessionYml
    /**
     * FIXME
     * This is currently not a unit test.
     * It relies on {@link TeeSecretsService} implementation to work.
     * This should be fixed.
     */
    @Test
    void shouldGetSessionYml() throws Exception {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        doReturn(getPreComputeTokens()).when(teeSecretsService)
                .getPreComputeTokens(request);
        doReturn(getAppTokens()).when(teeSecretsService)
                .getAppTokens(request);
        doReturn(getPostComputeTokens()).when(teeSecretsService)
                .getPostComputeTokens(request);

        when(teeWorkflowConfig.getPreComputeEntrypoint()).thenReturn(PRE_COMPUTE_ENTRYPOINT);
        when(teeWorkflowConfig.getPostComputeEntrypoint()).thenReturn(POST_COMPUTE_ENTRYPOINT);

        when(attestationSecurityConfig.getToleratedInsecureOptions())
                .thenReturn(List.of("hyperthreading", "debug-mode"));
        when(attestationSecurityConfig.getIgnoredSgxAdvisories())
                .thenReturn(List.of("INTEL-SA-00161", "INTEL-SA-00289"));

        String actualYmlString = palaemonSessionService.generateSession(request);
        Map<String, Object> actualYmlMap = new Yaml().load(actualYmlString);
        String expectedYamlString = FileHelper.readFile(EXPECTED_SESSION_FILE);
        Map<String, Object> expectedYmlMap = new Yaml().load(expectedYamlString);
        assertRecursively(expectedYmlMap, actualYmlMap);
    }
    //endregion
}
