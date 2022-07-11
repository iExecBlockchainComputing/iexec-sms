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

import com.iexec.common.precompute.PreComputeUtils;
import com.iexec.common.sms.secret.ReservedSecretKeyName;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.tee.TeeEnclaveConfigurationValidator;
import com.iexec.common.utils.FileHelper;
import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.common.worker.result.ResultUtils;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.secret.compute.OnChainObjectType;
import com.iexec.sms.secret.compute.SecretOwnerRole;
import com.iexec.sms.secret.compute.TeeTaskComputeSecret;
import com.iexec.sms.secret.compute.TeeTaskComputeSecretService;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.TeeSecretsService;
import com.iexec.sms.tee.session.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.TeeSessionGenerationException;
import com.iexec.sms.tee.session.scone.attestation.AttestationSecurityConfig;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import com.iexec.sms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.security.GeneralSecurityException;
import java.util.*;

import static com.iexec.common.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;
import static com.iexec.common.worker.result.ResultUtils.*;
import static com.iexec.sms.Web3jUtils.createEthereumAddress;
import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static com.iexec.sms.tee.session.TeeSessionTestUtils.APP_ENTRYPOINT;
import static com.iexec.sms.tee.session.scone.palaemon.PalaemonSessionService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class PalaemonSessionServiceTests {

    private static final String TEMPLATE_SESSION_FILE = "src/main/resources/palaemonTemplate.vm";
    private static final String EXPECTED_SESSION_FILE = "src/test/resources/tee-session.yml";
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

        String actualYmlString = palaemonSessionService.getSessionYml(request);
        Map<String, Object> actualYmlMap = new Yaml().load(actualYmlString);
        String expectedYamlString = FileHelper.readFile(EXPECTED_SESSION_FILE);
        Map<String, Object> expectedYmlMap = new Yaml().load(expectedYamlString);
        assertRecursively(expectedYmlMap, actualYmlMap);
    }
    //endregion
}
