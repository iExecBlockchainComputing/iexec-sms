/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.scone;

import com.iexec.common.utils.FileHelper;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.scone.cas.SconeSession;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SconeSessionMakerServiceTests {

    private static final String PRE_COMPUTE_ENTRYPOINT = "entrypoint1";
    private static final String APP_FINGERPRINT = "01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b";
    private static final String APP_ENTRYPOINT = "appEntrypoint";
    private static final String POST_COMPUTE_ENTRYPOINT = "entrypoint3";
    private static final String MAA_URL = "https://maa.attestation.service";

    private final TeeAppProperties preComputeProperties = TeeAppProperties.builder()
            .image("PRE_COMPUTE_IMAGE")
            .fingerprint(PRE_COMPUTE_FINGERPRINT)
            .entrypoint(PRE_COMPUTE_ENTRYPOINT)
            .heapSizeInBytes(1L)
            .build();
    private final TeeAppProperties postComputeProperties = TeeAppProperties.builder()
            .image("POST_COMPUTE_IMAGE")
            .fingerprint(POST_COMPUTE_FINGERPRINT)
            .entrypoint(POST_COMPUTE_ENTRYPOINT)
            .heapSizeInBytes(1L)
            .build();
    @Mock
    private SconeServicesProperties teeServicesConfig;
    @Mock
    private SecretSessionBaseService teeSecretsService;
    @Mock
    private SconeSessionSecurityConfig attestationSecurityConfig;

    @InjectMocks
    private SconeSessionMakerService palaemonSessionService;

    private TeeSessionRequest request;

    private void setupCommonMocks() throws TeeSessionGenerationException {
        when(teeServicesConfig.getPreComputeProperties()).thenReturn(preComputeProperties);
        when(teeServicesConfig.getPostComputeProperties()).thenReturn(postComputeProperties);

        TeeEnclaveConfiguration enclaveConfig = TeeEnclaveConfiguration.builder()
                .fingerprint(APP_FINGERPRINT)
                .entrypoint(APP_ENTRYPOINT)
                .build();
        request = createSessionRequest(createTaskDescription(enclaveConfig).build());

        SecretEnclaveBase preCompute = SecretEnclaveBase.builder()
                .name("pre-compute")
                .mrenclave("mrEnclave1")
                .environment(Map.ofEntries(
                        // Keeping these test env vars for now
                        // (could be less but keeping same resource file for now)
                        Map.entry("IEXEC_TASK_ID", "taskId"),
                        Map.entry("IEXEC_PRE_COMPUTE_OUT", "/iexec_in"),
                        Map.entry("IS_DATASET_REQUIRED", "true"),
                        Map.entry("IEXEC_DATASET_KEY", "datasetKey"),
                        Map.entry("IEXEC_DATASET_URL", "http://datasetUrl"),
                        Map.entry("IEXEC_DATASET_FILENAME", "datasetName"),
                        Map.entry("IEXEC_DATASET_CHECKSUM", "datasetChecksum"),
                        Map.entry("IEXEC_INPUT_FILES_FOLDER", "/iexec_in"),
                        Map.entry("IEXEC_INPUT_FILES_NUMBER", "2"),
                        Map.entry("IEXEC_INPUT_FILE_URL_1", "http://host/file1"),
                        Map.entry("IEXEC_INPUT_FILE_URL_2", "http://host/file2")))
                .build();
        SecretEnclaveBase appCompute = SecretEnclaveBase.builder()
                .name("app")
                .mrenclave(APP_FINGERPRINT)
                .environment(Map.ofEntries(
                        Map.entry("IEXEC_TASK_ID", "taskId"),
                        Map.entry("IEXEC_IN", "/iexec_in"),
                        Map.entry("IEXEC_OUT", "/iexec_out"),
                        Map.entry("IEXEC_DATASET_ADDRESS", "0xDatasetAddress"),
                        Map.entry("IEXEC_DATASET_FILENAME", "datasetName"),
                        Map.entry("IEXEC_BOT_SIZE", "1"),
                        Map.entry("IEXEC_BOT_FIRST_INDEX", "0"),
                        Map.entry("IEXEC_BOT_TASK_INDEX", "0"),
                        Map.entry("IEXEC_INPUT_FILES_FOLDER", "/iexec_in"),
                        Map.entry("IEXEC_INPUT_FILES_NUMBER", "2"),
                        Map.entry("IEXEC_INPUT_FILE_NAME_1", "file1"),
                        Map.entry("IEXEC_INPUT_FILE_NAME_2", "file2")))
                .build();
        SecretEnclaveBase postCompute = SecretEnclaveBase.builder()
                .name("post-compute")
                .mrenclave("mrEnclave3")
                .environment(Map.ofEntries(
                        Map.entry("RESULT_TASK_ID", "taskId"),
                        Map.entry("RESULT_ENCRYPTION", "yes"),
                        Map.entry("RESULT_ENCRYPTION_PUBLIC_KEY", "encryptionPublicKey"),
                        Map.entry("RESULT_STORAGE_PROVIDER", "ipfs"),
                        Map.entry("RESULT_STORAGE_PROXY", "storageProxy"),
                        Map.entry("RESULT_STORAGE_TOKEN", "storageToken"),
                        Map.entry("RESULT_STORAGE_CALLBACK", "no"),
                        Map.entry("RESULT_SIGN_WORKER_ADDRESS", "workerAddress"),
                        Map.entry("RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY", "teeChallengePrivateKey")))
                .build();

        when(teeSecretsService.getSecretsTokens(request))
                .thenReturn(SecretSessionBase.builder()
                        .preCompute(preCompute)
                        .appCompute(appCompute)
                        .postCompute(postCompute)
                        .build());

        when(attestationSecurityConfig.getToleratedInsecureOptions())
                .thenReturn(List.of("hyperthreading", "debug-mode"));
        when(attestationSecurityConfig.getIgnoredSgxAdvisories())
                .thenReturn(List.of("INTEL-SA-00161", "INTEL-SA-00289"));
    }

    // region HardwareModeTests
    @Nested
    class HardwareModeTests {
        @BeforeEach
        void setup() throws TeeSessionGenerationException {
            when(attestationSecurityConfig.getMode()).thenReturn("hardware");
            when(attestationSecurityConfig.getUrl()).thenReturn(null);
            palaemonSessionService = new SconeSessionMakerService(
                    teeSecretsService,
                    teeServicesConfig,
                    attestationSecurityConfig);
            setupCommonMocks();
        }

        @Test
        void shouldGenerateHardwareSession() throws Exception {
            SconeSession actualCasSession = palaemonSessionService.generateSession(request);
            log.info(actualCasSession.toString());
            Map<String, Object> actualYmlMap = new Yaml().load(actualCasSession.toString());
            String expectedYamlString = FileHelper.readFile("src/test/resources/palaemon-tee-session-hardware.yml");
            Map<String, Object> expectedYmlMap = new Yaml().load(expectedYamlString);
            assertRecursively(expectedYmlMap, actualYmlMap);
        }

        @Test
        void shouldNotSwitchFromHardwareToMaaMode() throws Exception {
            palaemonSessionService.generateSession(request);
            when(attestationSecurityConfig.getMode()).thenReturn("maa");
            when(attestationSecurityConfig.getUrl()).thenReturn(MAA_URL);
            assertThrows(IllegalStateException.class, () -> palaemonSessionService.generateSession(request));
        }
    }
    // endregion

    // region MaaModeTests
    @Nested
    class MaaModeTests {
        @BeforeEach
        void setup() throws TeeSessionGenerationException {
            when(attestationSecurityConfig.getMode()).thenReturn("maa");
            when(attestationSecurityConfig.getUrl()).thenReturn(MAA_URL);
            palaemonSessionService = new SconeSessionMakerService(
                    teeSecretsService,
                    teeServicesConfig,
                    attestationSecurityConfig);
            setupCommonMocks();
        }

        @Test
        void shouldGenerateMaaSession() throws Exception {
            SconeSession actualCasSession = palaemonSessionService.generateSession(request);
            log.info(actualCasSession.toString());
            Map<String, Object> actualYmlMap = new Yaml().load(actualCasSession.toString());
            String expectedYamlString = FileHelper.readFile("src/test/resources/palaemon-tee-session-maa.yml");
            Map<String, Object> expectedYmlMap = new Yaml().load(expectedYamlString);
            assertRecursively(expectedYmlMap, actualYmlMap);
        }

        @Test
        void shouldNotGenerateMaaSessionSinceNullURL() {
            when(attestationSecurityConfig.getUrl()).thenReturn(null);
            assertThrows(IllegalArgumentException.class, () -> palaemonSessionService.generateSession(request));
        }

        @Test
        void shouldNotSwitchFromMaaToHardwareMode() throws Exception {
            palaemonSessionService.generateSession(request);
            when(attestationSecurityConfig.getMode()).thenReturn("hardware");
            when(attestationSecurityConfig.getUrl()).thenReturn(null);
            assertThrows(IllegalStateException.class, () -> palaemonSessionService.generateSession(request));
        }
    }
    // endregion
}
