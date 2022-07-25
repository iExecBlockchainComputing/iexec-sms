package com.iexec.sms.tee.session;

import com.iexec.common.precompute.PreComputeUtils;
import com.iexec.common.sms.secret.ReservedSecretKeyName;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.tee.TeeEnclaveConfigurationValidator;
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
import com.iexec.sms.tee.session.generic.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import com.iexec.sms.utils.EthereumCredentials;
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

import java.security.GeneralSecurityException;
import java.util.*;

import static com.iexec.common.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.common.precompute.PreComputeUtils.IEXEC_DATASET_KEY;
import static com.iexec.common.precompute.PreComputeUtils.IS_DATASET_REQUIRED;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;
import static com.iexec.common.utils.IexecEnvUtils.*;
import static com.iexec.common.worker.result.ResultUtils.*;
import static com.iexec.sms.Web3jUtils.createEthereumAddress;
import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static com.iexec.sms.tee.session.TeeSecretsService.*;
import static com.iexec.sms.tee.session.TeeSecretsService.SESSION_ID;
import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TeeSecretsServiceTests {
    private static final String APP_FINGERPRINT = "01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b";

    private static final TeeEnclaveConfiguration enclaveConfig =
            mock(TeeEnclaveConfiguration.class);

    @Mock
    private Web3SecretService web3SecretService;
    @Mock
    private Web2SecretsService web2SecretsService;
    @Mock
    private TeeChallengeService teeChallengeService;
    @Mock
    private TeeWorkflowConfiguration teeWorkflowConfig;
    @Mock private TeeTaskComputeSecretService teeTaskComputeSecretService;

    @Spy
    @InjectMocks
    private TeeSecretsService teeSecretsService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        when(enclaveConfig.getFingerprint()).thenReturn(APP_FINGERPRINT);

        when(enclaveConfig.getFingerprint()).thenReturn(APP_FINGERPRINT);
        when(enclaveConfig.getEntrypoint()).thenReturn(APP_ENTRYPOINT);
    }

    //region getSecretsTokens
    @Test
    void shouldGetSecretsTokens() throws Exception {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        doReturn(getPreComputeTokens()).when(teeSecretsService)
                .getPreComputeTokens(request);
        doReturn(getAppTokens()).when(teeSecretsService)
                .getAppTokens(request);
        doReturn(getPostComputeTokens()).when(teeSecretsService)
                .getPostComputeTokens(request);

        Map<String, Object> actualTokens = teeSecretsService.getSecretsTokens(request);

        Map<String, Object> expectedEnvTokens = new HashMap<>();
        expectedEnvTokens.put(IEXEC_TASK_ID, "taskId");
        expectedEnvTokens.put(IEXEC_IN, "/iexec_in");
        expectedEnvTokens.put(IEXEC_OUT, "/iexec_out");
        expectedEnvTokens.put(IEXEC_INPUT_FILES_FOLDER, "/iexec_in");
        expectedEnvTokens.put("IEXEC_INPUT_FILE_NAME_1", "file1");
        expectedEnvTokens.put("IEXEC_INPUT_FILE_NAME_2", "file2");
        expectedEnvTokens.put(IEXEC_DATASET_URL, "http://datasetUrl");
        expectedEnvTokens.put(IEXEC_DATASET_FILENAME, "datasetName");
        expectedEnvTokens.put(IEXEC_DATASET_ADDRESS, "0xDatasetAddress");
        expectedEnvTokens.put(IEXEC_INPUT_FILES_NUMBER, "2");
        expectedEnvTokens.put("IEXEC_INPUT_FILE_URL_1", "http://host/file1");
        expectedEnvTokens.put("IEXEC_INPUT_FILE_URL_2", "http://host/file2");
        expectedEnvTokens.put(IEXEC_BOT_SIZE, "1");
        expectedEnvTokens.put(IEXEC_BOT_TASK_INDEX, "0");
        expectedEnvTokens.put(IEXEC_DATASET_CHECKSUM, "datasetChecksum");
        expectedEnvTokens.put(IEXEC_BOT_FIRST_INDEX, "0");


        Map<String, Object> expectedTokens = new HashMap<>();
        expectedTokens.put(APP_MRENCLAVE, "01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b");
        expectedTokens.put(PRE_COMPUTE_MRENCLAVE, "mrEnclave1");
        expectedTokens.put(POST_COMPUTE_MRENCLAVE, "mrEnclave3");
        expectedTokens.put(IS_DATASET_REQUIRED, true);
        expectedTokens.put(IEXEC_DATASET_KEY, "datasetKey");
        expectedTokens.put(IS_PRE_COMPUTE_REQUIRED, true);
        expectedTokens.put(RESULT_TASK_ID, "taskId");
        expectedTokens.put(RESULT_STORAGE_PROXY, "storageProxy");
        expectedTokens.put(RESULT_STORAGE_TOKEN, "storageToken");
        expectedTokens.put(RESULT_SIGN_WORKER_ADDRESS, request.getWorkerAddress());
        expectedTokens.put(RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, "teeChallengePrivateKey");
        expectedTokens.put(RESULT_STORAGE_PROVIDER, "ipfs");
        expectedTokens.put(RESULT_STORAGE_CALLBACK, "no");
        expectedTokens.put(RESULT_ENCRYPTION, "yes");
        expectedTokens.put(RESULT_ENCRYPTION_PUBLIC_KEY, "encryptionPublicKey");
        expectedTokens.put(INPUT_FILE_NAMES, Map.of(
                "IEXEC_INPUT_FILE_NAME_1", "file1",
                "IEXEC_INPUT_FILE_NAME_2", "file2"
        ));
        expectedTokens.put(INPUT_FILE_URLS, Map.of(
                "IEXEC_INPUT_FILE_URL_1", "http://host/file1",
                "IEXEC_INPUT_FILE_URL_2", "http://host/file2"
        ));
        expectedTokens.put("env", expectedEnvTokens);
        expectedTokens.put(SESSION_ID, "sessionId");

        assertRecursively(expectedTokens, actualTokens);
    }

    @Test
    void shouldNotGetSecretsTokensSinceRequestIsNull() {
        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getSecretsTokens(null)
        );
        assertEquals(NO_SESSION_REQUEST, exception.getError());
        assertEquals("Session request must not be null", exception.getMessage());
    }

    @Test
    void shouldNotGetSecretsTokensSinceTaskDescriptionIsMissing() {
        TeeSecretsSessionRequest request = TeeSecretsSessionRequest.builder().build();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getSecretsTokens(request)
        );
        assertEquals(NO_TASK_DESCRIPTION, exception.getError());
        assertEquals("Task description must not be null", exception.getMessage());
    }
    //endregion

    //region getPreComputeTokens
    @Test
    void shouldGetPreComputeTokens() throws Exception {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        when(teeWorkflowConfig.getPreComputeFingerprint())
                .thenReturn(PRE_COMPUTE_FINGERPRINT);
        when(teeWorkflowConfig.getPreComputeEntrypoint())
                .thenReturn(PRE_COMPUTE_ENTRYPOINT);
        Web3Secret secret = new Web3Secret(DATASET_ADDRESS, DATASET_KEY);
        when(web3SecretService.getSecret(DATASET_ADDRESS, true))
                .thenReturn(Optional.of(secret));

        Map<String, Object> tokens =
                teeSecretsService.getPreComputeTokens(request);
        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                PRE_COMPUTE_MRENCLAVE, PRE_COMPUTE_FINGERPRINT,
                                PreComputeUtils.IEXEC_DATASET_KEY, secret.getTrimmedValue(),
                                PreComputeUtils.IS_DATASET_REQUIRED, true,
                                INPUT_FILE_URLS,
                                Map.of(
                                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "1", INPUT_FILE_URL_1,
                                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "2", INPUT_FILE_URL_2)

                        )
                );
    }

    @Test
    void shouldGetPreComputeTokensWithoutDataset() throws Exception {
        TeeSecretsSessionRequest request = TeeSecretsSessionRequest.builder()
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
                teeSecretsService.getPreComputeTokens(request);
        assertThat(tokens).isNotEmpty()
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                PRE_COMPUTE_MRENCLAVE, PRE_COMPUTE_FINGERPRINT,
                                PreComputeUtils.IEXEC_DATASET_KEY, "",
                                PreComputeUtils.IS_DATASET_REQUIRED, false,
                                INPUT_FILE_URLS,
                                Map.of(
                                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "1", INPUT_FILE_URL_1,
                                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "2", INPUT_FILE_URL_2)

                        )
                );
    }
    //endregion

    //region getAppTokens
    @Test
    void shouldGetAppTokensForAdvancedTaskDescription() {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        String appAddress = request.getTaskDescription().getAppAddress();
        String requesterAddress = request.getTaskDescription().getRequester();

        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        when(validator.isValid()).thenReturn(true);
        addApplicationDeveloperSecret(appAddress);
        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_2, REQUESTER_SECRET_VALUE_2);

        Map<String, Object> tokens = assertDoesNotThrow(() -> teeSecretsService.getAppTokens(request));

        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", APP_DEVELOPER_SECRET_INDEX);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, REQUESTER_SECRET_KEY_1);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, REQUESTER_SECRET_KEY_2);

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                APP_MRENCLAVE, APP_FINGERPRINT,
                                INPUT_FILE_NAMES,
                                Map.of(
                                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "1", "file1",
                                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "2", "file2"
                                ),
                                IEXEC_APP_DEVELOPER_SECRET_1, APP_DEVELOPER_SECRET_VALUE,
                                REQUESTER_SECRETS,
                                Map.of(
                                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_1,
                                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "2", REQUESTER_SECRET_VALUE_2
                                )

                        )
                );
    }

    @Test
    void shouldGetTokensWithEmptyAppComputeSecretWhenSecretsDoNotExist() {
        final String appAddress = createEthereumAddress();
        final String requesterAddress = createEthereumAddress();
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .appUri(APP_URI)
                .appAddress(appAddress)
                .appEnclaveConfiguration(enclaveConfig)
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URL)
                .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .requester(requesterAddress)
                .cmd(ARGS)
                .inputFiles(List.of(INPUT_FILE_URL_1, INPUT_FILE_URL_2))
                .isResultEncryption(true)
                .resultStorageProvider(STORAGE_PROVIDER)
                .resultStorageProxy(STORAGE_PROXY)
                .botSize(1)
                .botFirstIndex(0)
                .botIndex(0)
                .build();
        TeeSecretsSessionRequest request = createSessionRequest(taskDescription);
        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        when(validator.isValid()).thenReturn(true);
        when(teeTaskComputeSecretService.getSecret(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(Optional.empty());

        Map<String, Object> tokens = assertDoesNotThrow(() -> teeSecretsService.getAppTokens(request));
        verify(teeTaskComputeSecretService).getSecret(eq(OnChainObjectType.APPLICATION), eq(appAddress), eq(SecretOwnerRole.APPLICATION_DEVELOPER), eq(""), any());
        verify(teeTaskComputeSecretService, never()).getSecret(eq(OnChainObjectType.APPLICATION), eq(""), eq(SecretOwnerRole.REQUESTER), any(), any());

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                APP_MRENCLAVE, APP_FINGERPRINT,
                                INPUT_FILE_NAMES,
                                Map.of(
                                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "1", "file1",
                                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "2", "file2"
                                ),
                                IEXEC_APP_DEVELOPER_SECRET_1, "",
                                REQUESTER_SECRETS, Collections.emptyMap()
                        )
                );
    }

    @Test
    void shouldFailToGetAppTokensSinceNoTaskDescription() {
        TeeSecretsSessionRequest request = TeeSecretsSessionRequest.builder()
                .build();
        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> teeSecretsService.getAppTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.NO_TASK_DESCRIPTION, exception.getError());
        Assertions.assertEquals("Task description must no be null", exception.getMessage());
    }

    @Test
    void shouldFailToGetAppTokensSinceNoEnclaveConfig() {
        TeeSecretsSessionRequest request = TeeSecretsSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(TaskDescription.builder().build())
                .build();
        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> teeSecretsService.getAppTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.APP_COMPUTE_NO_ENCLAVE_CONFIG, exception.getError());
        Assertions.assertEquals("Enclave configuration must no be null", exception.getMessage());
    }

    @Test
    void shouldFailToGetAppTokensInvalidEnclaveConfig() {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        String validationError = "validation error";
        when(validator.validate()).thenReturn(Collections.singletonList(validationError));
        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> teeSecretsService.getAppTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.APP_COMPUTE_INVALID_ENCLAVE_CONFIG, exception.getError());
    }

    @Test
    void shouldAddMultipleRequesterSecrets() {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        String requesterAddress = request.getTaskDescription().getRequester();

        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        when(validator.isValid()).thenReturn(true);
        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        addRequesterSecret(requesterAddress,  REQUESTER_SECRET_KEY_2, REQUESTER_SECRET_VALUE_2);
        Map<String, Object> tokens = assertDoesNotThrow(() -> teeSecretsService.getAppTokens(request));
        verify(teeTaskComputeSecretService, times(2))
                .getSecret(eq(OnChainObjectType.APPLICATION), eq(""), eq(SecretOwnerRole.REQUESTER), any(), any());
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, REQUESTER_SECRET_KEY_1);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, REQUESTER_SECRET_KEY_2);
        assertThat(tokens).containsEntry(REQUESTER_SECRETS,
                Map.of(
                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_1,
                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "2", REQUESTER_SECRET_VALUE_2
                ));
    }

    @Test
    void shouldFilterRequesterSecretIndexLowerThanZero() {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        String requesterAddress = request.getTaskDescription().getRequester();

        request.getTaskDescription().setSecrets(Map.of("1", REQUESTER_SECRET_KEY_1, "-1", "out-of-bound-requester-secret"));
        TeeEnclaveConfigurationValidator validator = mock(TeeEnclaveConfigurationValidator.class);
        when(enclaveConfig.getValidator()).thenReturn(validator);
        when(validator.isValid()).thenReturn(true);
        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        Map<String, Object> tokens = assertDoesNotThrow(() -> teeSecretsService.getAppTokens(request));
        verify(teeTaskComputeSecretService).getSecret(eq(OnChainObjectType.APPLICATION), eq(""), eq(SecretOwnerRole.REQUESTER), any(), any());
        assertThat(tokens).containsEntry(REQUESTER_SECRETS,
                Map.of(IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_1));
    }
    //endregion

    //region getPostComputeTokens
    @Test
    void shouldGetPostComputeTokens() throws Exception {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        String requesterAddress = request.getTaskDescription().getRequester();

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
                requesterAddress,
                ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN,
                true))
                .thenReturn(Optional.of(storageSecret));

        TeeChallenge challenge = TeeChallenge.builder()
                .credentials(EthereumCredentials.generate())
                .build();
        when(teeChallengeService.getOrCreate(TASK_ID, true))
                .thenReturn(Optional.of(challenge));


        Map<String, String> tokens =
                teeSecretsService.getPostComputeTokens(request);

        final Map<String, String> expectedTokens = new HashMap<>();
        expectedTokens.put(POST_COMPUTE_MRENCLAVE, POST_COMPUTE_FINGERPRINT);
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
    void shouldNotGetPostComputeTokensSinceTaskDescriptionMissing() {
        TeeSecretsSessionRequest request = TeeSecretsSessionRequest.builder().build();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeTokens(request)
        );
        assertEquals(NO_TASK_DESCRIPTION, exception.getError());
        assertEquals("Task description must not be null", exception.getMessage());
    }
    //endregion

    //region getPostComputeEncryptionTokens
    @Test
    void shouldGetPostComputeStorageTokensWithCallback() {
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        sessionRequest.getTaskDescription().setCallback("callback");

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest));

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
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();

        final String secretValue = "Secret value";
        when(web2SecretsService.getSecret(taskDescription.getRequester(), ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN, true))
                .thenReturn(Optional.of(new Secret(null, secretValue)));

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest));

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
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();
        taskDescription.setResultStorageProvider(DROPBOX_RESULT_STORAGE_PROVIDER);

        final String secretValue = "Secret value";
        when(web2SecretsService.getSecret(taskDescription.getRequester(), IEXEC_RESULT_DROPBOX_TOKEN, true))
                .thenReturn(Optional.of(new Secret(null, secretValue)));

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest));

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
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();

        when(web2SecretsService.getSecret(taskDescription.getRequester(), ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN, true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest));

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_STORAGE_TOKENS_FAILED);
        assertThat(exception.getMessage()).isEqualTo("Empty requester storage token - taskId: " + taskDescription.getChainTaskId());
    }

    @Test
    void shouldGetPostComputeSignTokens() throws GeneralSecurityException {
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();
        final String taskId = taskDescription.getChainTaskId();
        final EthereumCredentials credentials = EthereumCredentials.generate();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.of(TeeChallenge.builder().credentials(credentials).build()));

        final Map<String, String> tokens = assertDoesNotThrow(() -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

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
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();
        sessionRequest.setWorkerAddress(emptyWorkerAddress);

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_WORKER_ADDRESS);
        assertThat(exception.getMessage()).isEqualTo("Empty worker address - taskId: " + taskId);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveChallenge(String emptyEnclaveChallenge) {
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();
        sessionRequest.setEnclaveChallenge(emptyEnclaveChallenge);

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_PUBLIC_ENCLAVE_CHALLENGE);
        assertThat(exception.getMessage()).isEqualTo("Empty public enclave challenge - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoTeeChallenge() {
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CHALLENGE);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge  - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveCredentials() {
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.of(TeeChallenge.builder().credentials(null).build()));

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge credentials - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveCredentialsPrivateKey() {
        final TeeSecretsSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.of(TeeChallenge.builder().credentials(new EthereumCredentials("", "", false, "")).build()));

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest)
        );

        assertThat(exception.getError()).isEqualTo(POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge credentials - taskId: " + taskId);
    }

    @Test
    void shouldGetPostComputeEncryptionTokensWithEncryption() {
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        Secret publicKeySecret = new Secret("address", ENCRYPTION_PUBLIC_KEY);
        when(web2SecretsService.getSecret(
                request.getTaskDescription().getBeneficiary(),
                IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.of(publicKeySecret));

        final Map<String, String> encryptionTokens = assertDoesNotThrow(() -> teeSecretsService.getPostComputeEncryptionTokens(request));
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
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        request.getTaskDescription().setResultEncryption(false);

        final Map<String, String> encryptionTokens = assertDoesNotThrow(() -> teeSecretsService.getPostComputeEncryptionTokens(request));
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
        TeeSecretsSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        when(web2SecretsService.getSecret(
                request.getTaskDescription().getBeneficiary(),
                IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeEncryptionTokens(request)
        );
        assertEquals(POST_COMPUTE_GET_ENCRYPTION_TOKENS_FAILED_EMPTY_BENEFICIARY_KEY, exception.getError());
        assertEquals("Empty beneficiary encryption key - taskId: taskId", exception.getMessage());
    }

    //endregion

    //region utils
    private void addApplicationDeveloperSecret(String appAddress) {
        TeeTaskComputeSecret applicationDeveloperSecret = getApplicationDeveloperSecret(appAddress);
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(Optional.of(applicationDeveloperSecret));
    }

    private void addRequesterSecret(String requesterAddress, String secretKey, String secretValue) {
        TeeTaskComputeSecret requesterSecret = getRequesterSecret(requesterAddress, secretKey, secretValue);
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey))
                .thenReturn(Optional.of(requesterSecret));
    }
    //endregion
}