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

package com.iexec.sms.tee.session.palaemon;

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
import com.iexec.sms.tee.session.TeeSessionGenerationException;
import com.iexec.sms.tee.session.attestation.AttestationSecurityConfig;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.security.GeneralSecurityException;
import java.util.*;

import static com.iexec.common.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;
import static com.iexec.common.worker.result.ResultUtils.*;
import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static com.iexec.sms.tee.session.palaemon.PalaemonSessionService.INPUT_FILE_NAMES;
import static com.iexec.sms.tee.session.palaemon.PalaemonSessionService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class PalaemonSessionServiceTests {

    private static final String TEMPLATE_SESSION_FILE = "src/main/resources/palaemonTemplate.vm";
    private static final String EXPECTED_SESSION_FILE = "src/test/resources/tee-session.yml";

    private static final String TASK_ID = "taskId";
    private static final String SESSION_ID = "sessionId";
    private static final String WORKER_ADDRESS = "workerAddress";
    private static final String ENCLAVE_CHALLENGE = "enclaveChallenge";
    private static final String REQUESTER = "requester";
    // pre-compute
    private static final String PRE_COMPUTE_FINGERPRINT = "mrEnclave1";
    private static final String PRE_COMPUTE_ENTRYPOINT = "entrypoint1";
    private static final String DATASET_ADDRESS = "0xDatasetAddress";
    private static final String DATASET_NAME = "datasetName";
    private static final String DATASET_CHECKSUM = "datasetChecksum";
    private static final String DATASET_URL = "http://datasetUrl"; // 0x687474703a2f2f646174617365742d75726c in hex
    // keys with leading/trailing \n should not break the workflow
    private static final String DATASET_KEY = "\ndatasetKey\n";
    // app
    private static final String APP_DEVELOPER_SECRET_INDEX = "0";
    private static final String APP_DEVELOPER_SECRET_VALUE = "appDeveloperSecretValue";
    private static final String REQUESTER_SECRET_KEY_1 = "requesterSecretKey1";
    private static final String REQUESTER_SECRET_VALUE_1 = "requesterSecretValue1";
    private static final String REQUESTER_SECRET_KEY_2 = "requesterSecretKey2";
    private static final String REQUESTER_SECRET_VALUE_2 = "requesterSecretValue2";
    private static final String APP_URI = "appUri";
    private static final String APP_ADDRESS = "appAddress";
    private static final String APP_FINGERPRINT = "01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b";
    private static final String APP_ENTRYPOINT = "appEntrypoint";
    private static final TeeEnclaveConfiguration enclaveConfig =
            mock(TeeEnclaveConfiguration.class);
    private static final String ARGS = "args";
    private static final String IEXEC_APP_DEVELOPER_SECRET_0 = "IEXEC_APP_DEVELOPER_SECRET_0";
    // post-compute
    private static final String POST_COMPUTE_FINGERPRINT = "mrEnclave3";
    private static final String POST_COMPUTE_ENTRYPOINT = "entrypoint3";
    private static final String POST_COMPUTE_IMAGE = "postComputeImage";
    private static final String STORAGE_PROVIDER = "ipfs";
    private static final String STORAGE_PROXY = "storageProxy";
    private static final String STORAGE_TOKEN = "storageToken";
    private static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
    private static final String TEE_CHALLENGE_PRIVATE_KEY = "teeChallengePrivateKey";
    // input files
    private static final String INPUT_FILE_URL_1 = "http://host/file1";
    private static final String INPUT_FILE_NAME_1 = "file1";
    private static final String INPUT_FILE_URL_2 = "http://host/file2";
    private static final String INPUT_FILE_NAME_2 = "file2";

    @Mock
    private Web3SecretService web3SecretService;
    @Mock
    private Web2SecretsService web2SecretsService;
    @Mock
    private TeeChallengeService teeChallengeService;
    @Mock
    private TeeWorkflowConfiguration teeWorkflowConfig;
    @Mock
    private AttestationSecurityConfig attestationSecurityConfig;
    @Mock private TeeTaskComputeSecretService teeTaskComputeSecretService;

    private PalaemonSessionService palaemonSessionService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        // spy is needed to mock some internal calls of the tested
        // class when relevant
        palaemonSessionService = spy(new PalaemonSessionService(
                web3SecretService,
                web2SecretsService,
                teeChallengeService,
                teeWorkflowConfig,
                attestationSecurityConfig,
                teeTaskComputeSecretService
        ));
        ReflectionTestUtils.setField(palaemonSessionService, "palaemonTemplateFilePath", TEMPLATE_SESSION_FILE);
        when(enclaveConfig.getFingerprint()).thenReturn(APP_FINGERPRINT);
        when(enclaveConfig.getEntrypoint()).thenReturn(APP_ENTRYPOINT);
    }

    //region getSessionYml
    @Test
    void shouldGetSessionYml() throws Exception {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());
        doReturn(getPreComputeTokens()).when(palaemonSessionService)
                .getPreComputePalaemonTokens(request);
        doReturn(getAppTokens()).when(palaemonSessionService)
                .getAppPalaemonTokens(request);
        doReturn(getPostComputeTokens()).when(palaemonSessionService)
                .getPostComputePalaemonTokens(request);
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

    @Test
    void shouldNotGetSessionYmlSinceRequestIsNull() {
        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getSessionYml(null)
        );
        assertEquals(NO_SESSION_REQUEST, exception.getError());
        assertEquals("Session request must not be null", exception.getMessage());
    }

    @Test
    void shouldNotGetSessionYmlSinceTaskDescriptionIsMissing() {
        PalaemonSessionRequest request = PalaemonSessionRequest.builder().build();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getSessionYml(request)
        );
        assertEquals(NO_TASK_DESCRIPTION, exception.getError());
        assertEquals("Task description must not be null", exception.getMessage());
    }
    //endregion

    //region getPreComputePalaemonTokens
    @Test
    void shouldGetPreComputePalaemonTokens() throws Exception {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());
        when(teeWorkflowConfig.getPreComputeFingerprint())
                .thenReturn(PRE_COMPUTE_FINGERPRINT);
        when(teeWorkflowConfig.getPreComputeEntrypoint())
                .thenReturn(PRE_COMPUTE_ENTRYPOINT);
        Web3Secret secret = new Web3Secret(DATASET_ADDRESS, DATASET_KEY);
        when(web3SecretService.getSecret(DATASET_ADDRESS, true))
                .thenReturn(Optional.of(secret));

        Map<String, Object> tokens =
                palaemonSessionService.getPreComputePalaemonTokens(request);
        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                PalaemonSessionService.PRE_COMPUTE_MRENCLAVE, PRE_COMPUTE_FINGERPRINT,
                                PalaemonSessionService.PRE_COMPUTE_ENTRYPOINT, PRE_COMPUTE_ENTRYPOINT,
                                PreComputeUtils.IEXEC_DATASET_KEY, secret.getTrimmedValue(),
                                PreComputeUtils.IS_DATASET_REQUIRED, true,
                                PalaemonSessionService.INPUT_FILE_URLS,
                                Map.of(
                                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "1", INPUT_FILE_URL_1,
                                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "2", INPUT_FILE_URL_2)

                        )
                );
    }

    @Test
    void shouldGetPreComputePalaemonTokensWithoutDataset() throws Exception {
        PalaemonSessionRequest request = PalaemonSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(TaskDescription.builder()
                        .chainTaskId(TASK_ID)
                        .inputFiles(List.of(INPUT_FILE_URL_1, INPUT_FILE_URL_2))
                        .build())
                .build();
        when(teeWorkflowConfig.getPreComputeFingerprint())
                .thenReturn(PRE_COMPUTE_FINGERPRINT);
        when(teeWorkflowConfig.getPreComputeEntrypoint())
                .thenReturn(PRE_COMPUTE_ENTRYPOINT);

        Map<String, Object> tokens =
                palaemonSessionService.getPreComputePalaemonTokens(request);
        assertThat(tokens).isNotEmpty()
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                PalaemonSessionService.PRE_COMPUTE_MRENCLAVE, PRE_COMPUTE_FINGERPRINT,
                                PalaemonSessionService.PRE_COMPUTE_ENTRYPOINT, PRE_COMPUTE_ENTRYPOINT,
                                PreComputeUtils.IEXEC_DATASET_KEY, "",
                                PreComputeUtils.IS_DATASET_REQUIRED, false,
                                PalaemonSessionService.INPUT_FILE_URLS,
                                Map.of(
                                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "1", INPUT_FILE_URL_1,
                                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "2", INPUT_FILE_URL_2)

                        )
                );
    }
    //endregion

    //region getAppPalaemonTokens
    @Test
    void shouldGetAppPalaemonTokensForAdvancedTaskDescription() {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());
        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        when(validator.isValid()).thenReturn(true);
        addApplicationDeveloperSecret();
        addRequesterSecret(REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        addRequesterSecret(REQUESTER_SECRET_KEY_2, REQUESTER_SECRET_VALUE_2);

        Map<String, Object> tokens = assertDoesNotThrow(() -> palaemonSessionService.getAppPalaemonTokens(request));

        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", APP_DEVELOPER_SECRET_INDEX);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER, REQUESTER_SECRET_KEY_1);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER, REQUESTER_SECRET_KEY_2);

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                PalaemonSessionService.APP_MRENCLAVE, APP_FINGERPRINT,
                                PalaemonSessionService.APP_ARGS, APP_ENTRYPOINT + " " + ARGS,
                                PalaemonSessionService.INPUT_FILE_NAMES,
                                Map.of(
                                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "1", "file1",
                                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "2", "file2"
                                ),
                                IEXEC_APP_DEVELOPER_SECRET_0, APP_DEVELOPER_SECRET_VALUE,
                                REQUESTER_SECRETS,
                                Map.of(
                                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "0", REQUESTER_SECRET_VALUE_1,
                                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_2
                                )

                        )
                );
    }

    @Test
    void shouldGetPalaemonTokensWithEmptyAppComputeSecretWhenSecretsDoNotExist() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .appUri(APP_URI)
                .appAddress(APP_ADDRESS)
                .appEnclaveConfiguration(enclaveConfig)
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URL)
                .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .requester(REQUESTER)
                .cmd(ARGS)
                .inputFiles(List.of(INPUT_FILE_URL_1, INPUT_FILE_URL_2))
                .isResultEncryption(true)
                .resultStorageProvider(STORAGE_PROVIDER)
                .resultStorageProxy(STORAGE_PROXY)
                .botSize(1)
                .botFirstIndex(0)
                .botIndex(0)
                .build();
        PalaemonSessionRequest request = createSessionRequest(taskDescription);
        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        when(validator.isValid()).thenReturn(true);
        when(teeTaskComputeSecretService.getSecret(
                OnChainObjectType.APPLICATION,
                APP_ADDRESS,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(Optional.empty());

        Map<String, Object> tokens = assertDoesNotThrow(() -> palaemonSessionService.getAppPalaemonTokens(request));
        verify(teeTaskComputeSecretService).getSecret(eq(OnChainObjectType.APPLICATION), eq(APP_ADDRESS), eq(SecretOwnerRole.APPLICATION_DEVELOPER), eq(""), any());
        verify(teeTaskComputeSecretService, never()).getSecret(eq(OnChainObjectType.APPLICATION), eq(""), eq(SecretOwnerRole.REQUESTER), any(), any());

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                PalaemonSessionService.APP_MRENCLAVE, APP_FINGERPRINT,
                                PalaemonSessionService.APP_ARGS, APP_ENTRYPOINT + " " + ARGS,
                                PalaemonSessionService.INPUT_FILE_NAMES,
                                Map.of(
                                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "1", "file1",
                                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "2", "file2"
                                ),
                                IEXEC_APP_DEVELOPER_SECRET_0, "",
                                REQUESTER_SECRETS, Collections.emptyMap()
                        )
                );
    }

    @Test
    void shouldFailToGetAppPalaemonTokensSinceNoTaskDescription() {
        PalaemonSessionRequest request = PalaemonSessionRequest.builder()
                .build();
        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> palaemonSessionService.getAppPalaemonTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.NO_TASK_DESCRIPTION, exception.getError());
        Assertions.assertEquals("Task description must no be null", exception.getMessage());
    }

    @Test
    void shouldFailToGetAppPalaemonTokensSinceNoEnclaveConfig() {
        PalaemonSessionRequest request = PalaemonSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(TaskDescription.builder().build())
                .build();
        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> palaemonSessionService.getAppPalaemonTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.APP_COMPUTE_NO_ENCLAVE_CONFIG, exception.getError());
        Assertions.assertEquals("Enclave configuration must no be null", exception.getMessage());
    }

    @Test
    void shouldFailToGetAppPalaemonTokensInvalidEnclaveConfig() {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());
        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        String validationError = "validation error";
        when(validator.validate()).thenReturn(Collections.singletonList(validationError));
        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> palaemonSessionService.getAppPalaemonTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.APP_COMPUTE_INVALID_ENCLAVE_CONFIG, exception.getError());
    }

    @Test
    void shouldAddMultipleRequesterSecrets() {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());
        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        when(validator.isValid()).thenReturn(true);
        addRequesterSecret(REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        addRequesterSecret(REQUESTER_SECRET_KEY_2, REQUESTER_SECRET_VALUE_2);
        Map<String, Object> tokens = assertDoesNotThrow(() -> palaemonSessionService.getAppPalaemonTokens(request));
        verify(teeTaskComputeSecretService, times(2))
                .getSecret(eq(OnChainObjectType.APPLICATION), eq(""), eq(SecretOwnerRole.REQUESTER), any(), any());
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER, REQUESTER_SECRET_KEY_1);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER, REQUESTER_SECRET_KEY_2);
        assertThat(tokens).containsEntry(REQUESTER_SECRETS,
                Map.of(
                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "0", REQUESTER_SECRET_VALUE_1,
                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_2
                ));
    }

    @Test
    void shouldFilterRequesterSecretIndexLowerThanZero() {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());
        request.getTaskDescription().setSecrets(Map.of("0", REQUESTER_SECRET_KEY_1, "-1", "out-of-bound-requester-secret"));
        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        when(validator.isValid()).thenReturn(true);
        addRequesterSecret(REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        Map<String, Object> tokens = assertDoesNotThrow(() -> palaemonSessionService.getAppPalaemonTokens(request));
        verify(teeTaskComputeSecretService).getSecret(eq(OnChainObjectType.APPLICATION), eq(""), eq(SecretOwnerRole.REQUESTER), any(), any());
        assertThat(tokens).containsEntry(REQUESTER_SECRETS,
                Map.of(IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "0", REQUESTER_SECRET_VALUE_1));
    }
    //endregion

    //region getPostComputePalaemonTokens
    @Test
    void shouldGetPostComputePalaemonTokens() throws Exception {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());
        Secret publicKeySecret = new Secret("address", ENCRYPTION_PUBLIC_KEY);
        when(teeWorkflowConfig.getPostComputeFingerprint())
                .thenReturn(POST_COMPUTE_FINGERPRINT);
        when(teeWorkflowConfig.getPostComputeEntrypoint())
                .thenReturn(POST_COMPUTE_ENTRYPOINT);
        when(web2SecretsService.getSecret(
                request.getTaskDescription().getBeneficiary(),
                ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.of(publicKeySecret));
        Secret storageSecret = new Secret("address", STORAGE_TOKEN);
        when(web2SecretsService.getSecret(
                REQUESTER,
                ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN,
                true))
                .thenReturn(Optional.of(storageSecret));
        
        TeeChallenge challenge = TeeChallenge.builder()
                .credentials(EthereumCredentials.generate())
                .build();
        when(teeChallengeService.getOrCreate(TASK_ID, true))
                .thenReturn(Optional.of(challenge));


        Map<String, String> tokens =
                palaemonSessionService.getPostComputePalaemonTokens(request);

        final Map<String, String> expectedTokens = new HashMap<>();
        expectedTokens.put(PalaemonSessionService.POST_COMPUTE_MRENCLAVE, POST_COMPUTE_FINGERPRINT);
        expectedTokens.put(PalaemonSessionService.POST_COMPUTE_ENTRYPOINT, POST_COMPUTE_ENTRYPOINT);
        // encryption tokens
        expectedTokens.put(ResultUtils.RESULT_ENCRYPTION, "yes");
        expectedTokens.put(ResultUtils.RESULT_ENCRYPTION_PUBLIC_KEY, ENCRYPTION_PUBLIC_KEY);
        // storage tokens
        expectedTokens.put(ResultUtils.RESULT_STORAGE_CALLBACK, "no");
        expectedTokens.put(ResultUtils.RESULT_STORAGE_PROVIDER, STORAGE_PROVIDER);
        expectedTokens.put(ResultUtils.RESULT_STORAGE_PROXY, STORAGE_PROXY);
        expectedTokens.put(ResultUtils.RESULT_STORAGE_TOKEN, STORAGE_TOKEN);
        // sign tokens
        expectedTokens.put(ResultUtils.RESULT_TASK_ID, TASK_ID);
        expectedTokens.put(ResultUtils.RESULT_SIGN_WORKER_ADDRESS, WORKER_ADDRESS);
        expectedTokens.put(ResultUtils.RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, challenge.getCredentials().getPrivateKey());

        assertThat(tokens).containsExactlyEntriesOf(expectedTokens);
    }

    @Test
    void shouldNotGetPostComputePalaemonTokensSinceTaskDescriptionMissing() {
        PalaemonSessionRequest request = PalaemonSessionRequest.builder().build();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getPostComputePalaemonTokens(request)
        );
        assertEquals(NO_TASK_DESCRIPTION, exception.getError());
        assertEquals("Task description must not be null", exception.getMessage());
    }
    //endregion

    //region getPostComputeEncryptionTokens
    @Test
    void shouldGetPostComputeStorageTokensWithCallback() {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        sessionRequest.getTaskDescription().setCallback("callback");

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> palaemonSessionService.getPostComputeStorageTokens(sessionRequest));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                RESULT_STORAGE_CALLBACK, "yes",
                                RESULT_STORAGE_PROVIDER, EMPTY_YML_VALUE,
                                RESULT_STORAGE_PROXY, EMPTY_YML_VALUE,
                                RESULT_STORAGE_TOKEN, EMPTY_YML_VALUE
                        )
                );
    }

    @Test
    void shouldGetPostComputeStorageTokensOnIpfs() {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();

        final String secretValue = "Secret value";
        when(web2SecretsService.getSecret(taskDescription.getRequester(), ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN, true))
                .thenReturn(Optional.of(new Secret(null, secretValue)));

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> palaemonSessionService.getPostComputeStorageTokens(sessionRequest));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                RESULT_STORAGE_CALLBACK, "no",
                                RESULT_STORAGE_PROVIDER, STORAGE_PROVIDER,
                                RESULT_STORAGE_PROXY, STORAGE_PROXY,
                                RESULT_STORAGE_TOKEN, secretValue
                        )
                );
    }

    @Test
    void shouldGetPostComputeStorageTokensOnDropbox() {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();
        taskDescription.setResultStorageProvider(DROPBOX_RESULT_STORAGE_PROVIDER);

        final String secretValue = "Secret value";
        when(web2SecretsService.getSecret(taskDescription.getRequester(), IEXEC_RESULT_DROPBOX_TOKEN, true))
                .thenReturn(Optional.of(new Secret(null, secretValue)));

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> palaemonSessionService.getPostComputeStorageTokens(sessionRequest));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                RESULT_STORAGE_CALLBACK, "no",
                                RESULT_STORAGE_PROVIDER, DROPBOX_RESULT_STORAGE_PROVIDER,
                                RESULT_STORAGE_PROXY, STORAGE_PROXY,
                                RESULT_STORAGE_TOKEN, secretValue
                        )
                );
    }

    @Test
    void shouldNotGetPostComputeStorageTokensSinceNoSecret() {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();

        when(web2SecretsService.getSecret(taskDescription.getRequester(), ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN, true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getPostComputeStorageTokens(sessionRequest));

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_STORAGE_TOKENS_FAILED);
        assertThat(exception.getMessage()).isEqualTo("Empty requester storage token - taskId: " + taskDescription.getChainTaskId());
    }

    @Test
    void shouldGetPostComputeSignTokens() throws GeneralSecurityException {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();
        final String taskId = taskDescription.getChainTaskId();
        final EthereumCredentials credentials = EthereumCredentials.generate();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.of(TeeChallenge.builder().credentials(credentials).build()));

        final Map<String, String> tokens = assertDoesNotThrow(() -> palaemonSessionService.getPostComputeSignTokens(sessionRequest));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                RESULT_TASK_ID, taskId,
                                RESULT_SIGN_WORKER_ADDRESS, sessionRequest.getWorkerAddress(),
                                RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, credentials.getPrivateKey()
                        )
                );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldNotGetPostComputeSignTokensSinceNoWorkerAddress(String emptyWorkerAddress) {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();
        sessionRequest.setWorkerAddress(emptyWorkerAddress);

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_WORKER_ADDRESS);
        assertThat(exception.getMessage()).isEqualTo("Empty worker address - taskId: " + taskId);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveChallenge(String emptyEnclaveChallenge) {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();
        sessionRequest.setEnclaveChallenge(emptyEnclaveChallenge);

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_PUBLIC_ENCLAVE_CHALLENGE);
        assertThat(exception.getMessage()).isEqualTo("Empty public enclave challenge - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoTeeChallenge() {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CHALLENGE);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge  - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveCredentials() {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.of(TeeChallenge.builder().credentials(null).build()));

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge credentials - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveCredentialsPrivateKey() {
        final PalaemonSessionRequest sessionRequest = createSessionRequest(createTaskDescription());
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.of(TeeChallenge.builder().credentials(new EthereumCredentials("", "", false, "")).build()));

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge credentials - taskId: " + taskId);
    }

    // endregion

    @Test
    void shouldGetPostComputeEncryptionTokensWithEncryption() {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());

        Secret publicKeySecret = new Secret("address", ENCRYPTION_PUBLIC_KEY);
        when(web2SecretsService.getSecret(
                request.getTaskDescription().getBeneficiary(),
                IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.of(publicKeySecret));

        final Map<String, String> encryptionTokens = assertDoesNotThrow(() -> palaemonSessionService.getPostComputeEncryptionTokens(request));
        assertThat(encryptionTokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                RESULT_ENCRYPTION, "yes",
                                RESULT_ENCRYPTION_PUBLIC_KEY, ENCRYPTION_PUBLIC_KEY
                        )
                );
    }

    @Test
    void shouldGetPostComputeEncryptionTokensWithoutEncryption() {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());
        request.getTaskDescription().setResultEncryption(false);

        final Map<String, String> encryptionTokens = assertDoesNotThrow(() -> palaemonSessionService.getPostComputeEncryptionTokens(request));
        assertThat(encryptionTokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                RESULT_ENCRYPTION, "no",
                                RESULT_ENCRYPTION_PUBLIC_KEY, ""
                        )
                );
    }

    @Test
    void shouldNotGetPostComputeEncryptionTokensSinceEmptyBeneficiaryKey() {
        PalaemonSessionRequest request = createSessionRequest(createTaskDescription());

        when(web2SecretsService.getSecret(
                request.getTaskDescription().getBeneficiary(),
                IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> palaemonSessionService.getPostComputeEncryptionTokens(request)
        );
        assertEquals(POST_COMPUTE_GET_ENCRYPTION_TOKENS_FAILED_EMPTY_BENEFICIARY_KEY, exception.getError());
        assertEquals("Empty beneficiary encryption key - taskId: taskId", exception.getMessage());
    }

    //endregion

    //region utils
    private void addApplicationDeveloperSecret() {
        TeeTaskComputeSecret applicationDeveloperSecret = TeeTaskComputeSecret.builder()
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .onChainObjectAddress(APP_ADDRESS)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .key(APP_DEVELOPER_SECRET_INDEX)
                .value(APP_DEVELOPER_SECRET_VALUE)
                .build();
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(Optional.of(applicationDeveloperSecret));
    }

    private void addRequesterSecret(String secretKey, String secretValue) {
        TeeTaskComputeSecret requesterSecret = TeeTaskComputeSecret.builder()
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .onChainObjectAddress("")
                .secretOwnerRole(SecretOwnerRole.REQUESTER)
                .fixedSecretOwner(REQUESTER)
                .key(secretKey)
                .value(secretValue)
                .build();
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER, secretKey))
                .thenReturn(Optional.of(requesterSecret));
    }

    private PalaemonSessionRequest createSessionRequest(TaskDescription taskDescription) {
        return PalaemonSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(taskDescription)
                .build();
    }

    private TaskDescription createTaskDescription() {
        return TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .appUri(APP_URI)
                .appAddress(APP_ADDRESS)
                .appEnclaveConfiguration(enclaveConfig)
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URL)
                .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .requester(REQUESTER)
                .cmd(ARGS)
                .inputFiles(List.of(INPUT_FILE_URL_1, INPUT_FILE_URL_2))
                .isResultEncryption(true)
                .resultStorageProvider(STORAGE_PROVIDER)
                .resultStorageProxy(STORAGE_PROXY)
                .secrets(Map.of("0", REQUESTER_SECRET_KEY_1, "1", REQUESTER_SECRET_KEY_2))
                .botSize(1)
                .botFirstIndex(0)
                .botIndex(0)
                .build();
    }

    private Map<String, Object> getPreComputeTokens() {
        return Map.of(
                PRE_COMPUTE_MRENCLAVE, PRE_COMPUTE_FINGERPRINT,
                PalaemonSessionService.PRE_COMPUTE_ENTRYPOINT, PRE_COMPUTE_ENTRYPOINT,
                PreComputeUtils.IS_DATASET_REQUIRED, true,
                PreComputeUtils.IEXEC_DATASET_KEY, DATASET_KEY.trim(),
                INPUT_FILE_URLS, Map.of(
                    IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "1", INPUT_FILE_URL_1,
                    IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "2", INPUT_FILE_URL_2));
    }

    private Map<String, Object> getAppTokens() {
        return Map.of(
                APP_MRENCLAVE, APP_FINGERPRINT,
                APP_ARGS, APP_ENTRYPOINT + " " + ARGS,
                INPUT_FILE_NAMES, Map.of(
                    IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "1", INPUT_FILE_NAME_1,
                    IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "2", INPUT_FILE_NAME_2));
    }

    private Map<String, Object> getPostComputeTokens() {
        Map<String, Object> map = new HashMap<>();
        map.put(POST_COMPUTE_MRENCLAVE, POST_COMPUTE_FINGERPRINT);
        map.put(PalaemonSessionService.POST_COMPUTE_ENTRYPOINT, POST_COMPUTE_ENTRYPOINT);
        map.put(RESULT_TASK_ID, TASK_ID);
        map.put(RESULT_ENCRYPTION, "yes");
        map.put(RESULT_ENCRYPTION_PUBLIC_KEY, ENCRYPTION_PUBLIC_KEY);
        map.put(RESULT_STORAGE_PROVIDER, STORAGE_PROVIDER);
        map.put(RESULT_STORAGE_PROXY, STORAGE_PROXY);
        map.put(RESULT_STORAGE_TOKEN, STORAGE_TOKEN);
        map.put(RESULT_STORAGE_CALLBACK, "no");
        map.put(RESULT_SIGN_WORKER_ADDRESS, WORKER_ADDRESS);
        map.put(RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, TEE_CHALLENGE_PRIVATE_KEY);
        return map;
    }

    private void assertRecursively(Object expected, Object actual) {
        if (expected == null ||
                expected instanceof String ||
                ClassUtils.isPrimitiveOrWrapper(expected.getClass())) {
            log.info("Comparing [actual:{}, expected:{}]", expected, actual);
            assertThat(expected).isEqualTo(actual);
            return;
        }
        if (expected instanceof List) {
            List<?> actualList = (List<?>) expected;
            List<?> expectedList = (List<?>) actual;
            for (int i = 0; i < actualList.size(); i++) {
                assertRecursively(actualList.get(i), expectedList.get(i));
            }
            return;
        }
        if (expected instanceof Map) {
            Map<?, ?> actualMap = (Map<?, ?>) expected;
            Map<?, ?> expectedMap = (Map<?, ?>) actual;
            actualMap.keySet().forEach((key) -> {
                log.info("Checking '{}'", key);
                assertRecursively(actualMap.get(key), expectedMap.get(key));
            });
        }
    }
    //endregion
}
