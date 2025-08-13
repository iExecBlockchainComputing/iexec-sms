/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.utils.FeignBuilder;
import com.iexec.common.utils.FileHelper;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.scone.cas.SconeSession;
import feign.Feign;
import feign.FeignException;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yaml.snakeyaml.Yaml;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.createSessionRequest;
import static com.iexec.sms.tee.session.TeeSessionTestUtils.createTaskDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SconeSessionMakerServiceTests {

    private static final String APP_FINGERPRINT = "01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b";
    private static final String APP_ENTRYPOINT = "appEntrypoint";
    private static final URL MAA_URL;
    private static final URL MAA_URL_OUT_OF_SERVICE;

    static {
        try {
            MAA_URL_OUT_OF_SERVICE = new URL("https://broken.maa.attestation.service");
            MAA_URL = new URL("https://maa.attestation.service");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final List<String> toleratedInsecureOptions = List.of("hyperthreading", "debug-mode");
    private final List<String> ignoredSgxAdvisories = List.of("INTEL-SA-00161", "INTEL-SA-00289");

    @Mock
    private SecretSessionBaseService teeSecretsService;

    @Mock
    private Feign.Builder feignClientBuilder;

    @Mock
    private AzureAttestationServer mockedAttestationServer;

    @Mock
    private AzureAttestationServer outOfServiceAttestationServer;

    private SconeSessionMakerService sconeSessionMakerService;

    private TeeSessionRequest request;

    private void setupCommonMocks(final String mode, final URL url, final List<URL> urls) throws TeeSessionGenerationException {
        final SconeSessionSecurityConfig attestationSecurityConfig = new SconeSessionSecurityConfig(
                toleratedInsecureOptions, ignoredSgxAdvisories, mode, url, urls);
        sconeSessionMakerService = new SconeSessionMakerService(
                teeSecretsService,
                attestationSecurityConfig
        );

        final TeeEnclaveConfiguration enclaveConfig = TeeEnclaveConfiguration.builder()
                .fingerprint(APP_FINGERPRINT)
                .entrypoint(APP_ENTRYPOINT)
                .build();
        request = createSessionRequest(createTaskDescription(enclaveConfig).build());

        final SecretEnclaveBase preCompute = SecretEnclaveBase.builder()
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
        final SecretEnclaveBase appCompute = SecretEnclaveBase.builder()
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
        final SecretEnclaveBase postCompute = SecretEnclaveBase.builder()
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
    }

    @Test
    void shouldGenerateHardwareSession() throws Exception {
        setupCommonMocks("hardware", null, List.of());
        final SconeSession actualCasSession = sconeSessionMakerService.generateSession(request);
        log.info(actualCasSession.toString());
        final Map<String, Object> actualYmlMap = new Yaml().load(actualCasSession.toString());
        final String expectedYamlString = FileHelper.readFile("src/test/resources/palaemon-tee-session-hardware.yml");
        final Map<String, Object> expectedYmlMap = new Yaml().load(expectedYamlString);
        assertThat(actualYmlMap)
                .usingRecursiveComparison()
                .isEqualTo(expectedYmlMap);
    }

    @Test
    void shouldGenerateMaaSession() throws Exception {
        try (MockedStatic<FeignBuilder> feignBuilder = mockStatic(FeignBuilder.class)) {
            feignBuilder.when(() -> FeignBuilder.createBuilder(Logger.Level.BASIC))
                    .thenReturn(feignClientBuilder);
            when(feignClientBuilder.target(AzureAttestationServer.class, MAA_URL.toString()))
                    .thenReturn(mockedAttestationServer);
            setupCommonMocks("maa", MAA_URL, List.of(MAA_URL));
            final SconeSession actualCasSession = sconeSessionMakerService.generateSession(request);
            log.info(actualCasSession.toString());
            final Map<String, Object> actualYmlMap = new Yaml().load(actualCasSession.toString());
            final String expectedYamlString = FileHelper.readFile("src/test/resources/palaemon-tee-session-maa.yml");
            final Map<String, Object> expectedYmlMap = new Yaml().load(expectedYamlString);
            assertThat(actualYmlMap)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedYmlMap);
        }
    }

    /**
     * This test is repeated 10 times to have both situations where the unhealthy server is queried or not.
     *
     * @throws Exception if an exception occurs during test
     */
    @RepeatedTest(10)
    void shouldUseAnotherMaaServerWhenOneIsDown() throws Exception {
        try (MockedStatic<FeignBuilder> feignBuilder = mockStatic(FeignBuilder.class)) {
            feignBuilder.when(() -> FeignBuilder.createBuilder(Logger.Level.BASIC))
                    .thenReturn(feignClientBuilder);
            when(feignClientBuilder.target(AzureAttestationServer.class, MAA_URL.toString()))
                    .thenReturn(mockedAttestationServer);
            when(mockedAttestationServer.canFetchOpenIdMetadata())
                    .thenReturn("");
            when(feignClientBuilder.target(AzureAttestationServer.class, MAA_URL_OUT_OF_SERVICE.toString()))
                    .thenReturn(outOfServiceAttestationServer);
            // Due to shuffling the collection, the test may end before the OUT-OF-SERVICE server is queried
            lenient().when(outOfServiceAttestationServer.canFetchOpenIdMetadata())
                    .thenThrow(FeignException.TooManyRequests.class);
            setupCommonMocks("maa", null, List.of(MAA_URL, MAA_URL_OUT_OF_SERVICE));
            final SconeSession actualCasSession = sconeSessionMakerService.generateSession(request);
            log.info(actualCasSession.toString());
            final Map<String, Object> actualYmlMap = new Yaml().load(actualCasSession.toString());
            final String expectedYamlString = FileHelper.readFile("src/test/resources/palaemon-tee-session-maa.yml");
            final Map<String, Object> expectedYmlMap = new Yaml().load(expectedYamlString);
            assertThat(actualYmlMap)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedYmlMap);
            verify(outOfServiceAttestationServer, atMostOnce()).canFetchOpenIdMetadata();
            verify(mockedAttestationServer).canFetchOpenIdMetadata();
        }
    }
}
