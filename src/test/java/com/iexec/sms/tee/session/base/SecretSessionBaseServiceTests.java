package com.iexec.sms.tee.session.base;

import com.iexec.common.chain.DealParams;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.tee.TeeFramework;
import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.secret.ReservedSecretKeyName;
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
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
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

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecretSessionBaseServiceTests {

    private static final TeeEnclaveConfiguration enclaveConfig = TeeEnclaveConfiguration.builder()
            .framework(TeeFramework.SCONE)// any would be fine
            .entrypoint(APP_ENTRYPOINT)
            .fingerprint(APP_FINGERPRINT)
            .heapSize(1)
            .build();

    @Mock
    private Web3SecretService web3SecretService;
    @Mock
    private Web2SecretsService web2SecretsService;
    @Mock
    private TeeChallengeService teeChallengeService;
    @Mock
    private TeeAppProperties preComputeProperties;
    @Mock
    private TeeAppProperties postComputeProperties;
    @Mock
    private TeeServicesProperties teeServicesConfig;
    @Mock
    private TeeTaskComputeSecretService teeTaskComputeSecretService;

    @InjectMocks
    private SecretSessionBaseService teeSecretsService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        when(teeServicesConfig.getPreComputeProperties()).thenReturn(preComputeProperties);
        when(teeServicesConfig.getPostComputeProperties()).thenReturn(postComputeProperties);
    }

    // region getSecretsTokens
    @Test
    void shouldGetSecretsTokens() throws Exception {
        TaskDescription taskDescription = createTaskDescription(enclaveConfig);
        TeeSessionRequest request = createSessionRequest(taskDescription);

        // pre
        when(preComputeProperties.getFingerprint())
                .thenReturn(PRE_COMPUTE_FINGERPRINT);
        Web3Secret secret = new Web3Secret(DATASET_ADDRESS, DATASET_KEY);
        when(web3SecretService.getSecret(DATASET_ADDRESS, true))
                .thenReturn(Optional.of(secret));
        // post
        Secret publicKeySecret = new Secret("address", ENCRYPTION_PUBLIC_KEY);
        when(postComputeProperties.getFingerprint())
                .thenReturn(POST_COMPUTE_FINGERPRINT);
        when(web2SecretsService.getSecret(
                request.getTaskDescription().getBeneficiary(),
                ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.of(publicKeySecret));
        Secret storageSecret = new Secret("address", STORAGE_TOKEN);
        when(web2SecretsService.getSecret(taskDescription.getRequester(),
                ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN, true))
                .thenReturn(Optional.of(storageSecret));
        TeeChallenge challenge = TeeChallenge.builder()
                .credentials(EthereumCredentials.generate())
                .build();
        when(teeChallengeService.getOrCreate(TASK_ID, true))
                .thenReturn(Optional.of(challenge));

        SecretSessionBase sessionBase = teeSecretsService.getSecretsTokens(request);

        SecretEnclaveBase preComputeBase = sessionBase.getPreCompute();
        assertEquals("pre-compute", preComputeBase.getName());
        assertEquals(PRE_COMPUTE_FINGERPRINT, preComputeBase.getMrenclave());
        // environment content checks are handled in dedicated tests below
        assertEquals(teeSecretsService.getPreComputeTokens(request).getEnvironment(),
                preComputeBase.getEnvironment());

        SecretEnclaveBase appComputeBase = sessionBase.getAppCompute();
        assertEquals("app", appComputeBase.getName());
        assertEquals(APP_FINGERPRINT, appComputeBase.getMrenclave());
        // environment content checks are handled in dedicated tests below
        assertEquals(teeSecretsService.getAppTokens(request).getEnvironment(),
                appComputeBase.getEnvironment());

        SecretEnclaveBase postComputeBase = sessionBase.getPostCompute();
        assertEquals("post-compute", postComputeBase.getName());
        assertEquals(POST_COMPUTE_FINGERPRINT, postComputeBase.getMrenclave());
        // environment content checks are handled in dedicated tests below
        assertEquals(teeSecretsService.getPostComputeTokens(request).getEnvironment(),
                postComputeBase.getEnvironment());
    }

    @Test
    void shouldNotGetSecretsTokensSinceRequestIsNull() {
        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getSecretsTokens(null));
        assertEquals(TeeSessionGenerationError.NO_SESSION_REQUEST, exception.getError());
        assertEquals("Session request must not be null", exception.getMessage());
    }

    @Test
    void shouldNotGetSecretsTokensSinceTaskDescriptionIsMissing() {
        TeeSessionRequest request = TeeSessionRequest.builder().build();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getSecretsTokens(request));
        assertEquals(TeeSessionGenerationError.NO_TASK_DESCRIPTION, exception.getError());
        assertEquals("Task description must not be null", exception.getMessage());
    }
    // endregion

    // region getPreComputeTokens
    @Test
    void shouldGetPreComputeTokens() throws Exception {
        TaskDescription taskDescription = createTaskDescription(enclaveConfig);
        TeeSessionRequest request = createSessionRequest(taskDescription);
        when(preComputeProperties.getFingerprint())
                .thenReturn(PRE_COMPUTE_FINGERPRINT);
        Web3Secret secret = new Web3Secret(DATASET_ADDRESS, DATASET_KEY);
        when(web3SecretService.getSecret(DATASET_ADDRESS, true))
                .thenReturn(Optional.of(secret));

        SecretEnclaveBase enclaveBase = teeSecretsService.getPreComputeTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("pre-compute");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(PRE_COMPUTE_FINGERPRINT);
        Map<String, Object> expectedTokens = new HashMap<>();
        expectedTokens.put("IEXEC_TASK_ID", TASK_ID);
        expectedTokens.put("IEXEC_PRE_COMPUTE_OUT", "/iexec_in");
        expectedTokens.put("IS_DATASET_REQUIRED", true);
        expectedTokens.put("IEXEC_DATASET_KEY", secret.getTrimmedValue());
        expectedTokens.put("IEXEC_DATASET_URL", DATASET_URL);
        expectedTokens.put("IEXEC_DATASET_FILENAME", DATASET_NAME);
        expectedTokens.put("IEXEC_DATASET_CHECKSUM", DATASET_CHECKSUM);
        expectedTokens.put("IEXEC_INPUT_FILES_FOLDER", "/iexec_in");
        expectedTokens.put("IEXEC_INPUT_FILES_NUMBER", "2");
        expectedTokens.put("IEXEC_INPUT_FILE_URL_1", INPUT_FILE_URL_1);
        expectedTokens.put("IEXEC_INPUT_FILE_URL_2", INPUT_FILE_URL_2);
        assertThat(enclaveBase.getEnvironment()).containsExactlyInAnyOrderEntriesOf(expectedTokens);
    }

    @Test
    void shouldGetPreComputeTokensWithoutDataset() throws Exception {
        TeeSessionRequest request = TeeSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(TaskDescription.builder()
                        .chainTaskId(TASK_ID)
                        .inputFiles(List.of(INPUT_FILE_URL_1, INPUT_FILE_URL_2))
                        .build())
                .build();
        when(preComputeProperties.getFingerprint())
                .thenReturn(PRE_COMPUTE_FINGERPRINT);

        SecretEnclaveBase enclaveBase = teeSecretsService.getPreComputeTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("pre-compute");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(PRE_COMPUTE_FINGERPRINT);
        Map<String, Object> expectedTokens = new HashMap<>();
        expectedTokens.put("IEXEC_TASK_ID", TASK_ID);
        expectedTokens.put("IEXEC_PRE_COMPUTE_OUT", "/iexec_in");
        expectedTokens.put("IS_DATASET_REQUIRED", false);
        expectedTokens.put("IEXEC_INPUT_FILES_FOLDER", "/iexec_in");
        expectedTokens.put("IEXEC_INPUT_FILES_NUMBER", "2");
        expectedTokens.put("IEXEC_INPUT_FILE_URL_1", INPUT_FILE_URL_1);
        expectedTokens.put("IEXEC_INPUT_FILE_URL_2", INPUT_FILE_URL_2);
        assertThat(enclaveBase.getEnvironment()).containsExactlyInAnyOrderEntriesOf(expectedTokens);
    }
    // endregion

    // region getAppTokens
    @Test
    void shouldGetAppTokensForAdvancedTaskDescription() throws TeeSessionGenerationException {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        String appAddress = request.getTaskDescription().getAppAddress();
        String requesterAddress = request.getTaskDescription().getRequester();

        addApplicationDeveloperSecret(appAddress);
        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_2, REQUESTER_SECRET_VALUE_2);

        SecretEnclaveBase enclaveBase = teeSecretsService.getAppTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("app");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(APP_FINGERPRINT);
        Map<String, Object> expectedTokens = new HashMap<>();
        expectedTokens.put("IEXEC_TASK_ID", TASK_ID);
        expectedTokens.put("IEXEC_IN", "/iexec_in");
        expectedTokens.put("IEXEC_OUT", "/iexec_out");
        expectedTokens.put("IEXEC_DATASET_ADDRESS", DATASET_ADDRESS);
        expectedTokens.put("IEXEC_DATASET_FILENAME", DATASET_NAME);
        expectedTokens.put("IEXEC_BOT_SIZE", "1");
        expectedTokens.put("IEXEC_BOT_FIRST_INDEX", "0");
        expectedTokens.put("IEXEC_BOT_TASK_INDEX", "0");
        expectedTokens.put("IEXEC_INPUT_FILES_FOLDER", "/iexec_in");
        expectedTokens.put("IEXEC_INPUT_FILES_NUMBER", "2");
        expectedTokens.put("IEXEC_INPUT_FILE_NAME_1", INPUT_FILE_NAME_1);
        expectedTokens.put("IEXEC_INPUT_FILE_NAME_2", INPUT_FILE_NAME_2);
        expectedTokens.put("IEXEC_APP_DEVELOPER_SECRET", APP_DEVELOPER_SECRET_VALUE);
        expectedTokens.put("IEXEC_APP_DEVELOPER_SECRET_1", APP_DEVELOPER_SECRET_VALUE);
        expectedTokens.put("IEXEC_REQUESTER_SECRET_1", REQUESTER_SECRET_VALUE_1);
        expectedTokens.put("IEXEC_REQUESTER_SECRET_2", REQUESTER_SECRET_VALUE_2);
        assertThat(enclaveBase.getEnvironment()).containsExactlyInAnyOrderEntriesOf(expectedTokens);

        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER, "", APP_DEVELOPER_SECRET_INDEX);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER,
                requesterAddress, REQUESTER_SECRET_KEY_1);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER,
                requesterAddress, REQUESTER_SECRET_KEY_2);
    }

    @Test
    void shouldGetTokensWithEmptyAppComputeSecretWhenSecretsDoNotExist() throws TeeSessionGenerationException {
        final String appAddress = "0xapp";
        final String requesterAddress = "0xrequester";
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
        TeeSessionRequest request = createSessionRequest(taskDescription);

        when(teeTaskComputeSecretService.getSecret(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(Optional.empty());

        SecretEnclaveBase enclaveBase = teeSecretsService.getAppTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("app");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(APP_FINGERPRINT);
        Map<String, Object> expectedTokens = new HashMap<>();
        expectedTokens.put("IEXEC_TASK_ID", TASK_ID);
        expectedTokens.put("IEXEC_IN", "/iexec_in");
        expectedTokens.put("IEXEC_OUT", "/iexec_out");
        expectedTokens.put("IEXEC_DATASET_ADDRESS", DATASET_ADDRESS);
        expectedTokens.put("IEXEC_DATASET_FILENAME", DATASET_NAME);
        expectedTokens.put("IEXEC_BOT_SIZE", "1");
        expectedTokens.put("IEXEC_BOT_FIRST_INDEX", "0");
        expectedTokens.put("IEXEC_BOT_TASK_INDEX", "0");
        expectedTokens.put("IEXEC_INPUT_FILES_FOLDER", "/iexec_in");
        expectedTokens.put("IEXEC_INPUT_FILES_NUMBER", "2");
        expectedTokens.put("IEXEC_INPUT_FILE_NAME_1", INPUT_FILE_NAME_1);
        expectedTokens.put("IEXEC_INPUT_FILE_NAME_2", INPUT_FILE_NAME_2);
        assertThat(enclaveBase.getEnvironment()).containsExactlyInAnyOrderEntriesOf(expectedTokens);

        verify(teeTaskComputeSecretService).getSecret(eq(OnChainObjectType.APPLICATION), eq(appAddress),
                eq(SecretOwnerRole.APPLICATION_DEVELOPER), eq(""), any());
        verify(teeTaskComputeSecretService, never()).getSecret(eq(OnChainObjectType.APPLICATION), eq(""),
                eq(SecretOwnerRole.REQUESTER), any(), any());
    }

    @Test
    void shouldFailToGetAppTokensSinceNoTaskDescription() {
        TeeSessionRequest request = TeeSessionRequest.builder()
                .build();
        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> teeSecretsService.getAppTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.NO_TASK_DESCRIPTION, exception.getError());
        Assertions.assertEquals("Task description must not be null", exception.getMessage());
    }

    @Test
    void shouldFailToGetAppTokensSinceNoEnclaveConfig() {
        TeeSessionRequest request = TeeSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(TaskDescription.builder().build())
                .build();
        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> teeSecretsService.getAppTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.APP_COMPUTE_NO_ENCLAVE_CONFIG, exception.getError());
        Assertions.assertEquals("Enclave configuration must not be null", exception.getMessage());
    }

    @Test
    void shouldFailToGetAppTokensInvalidEnclaveConfig() {
        // invalid enclave config
        TeeSessionRequest request = createSessionRequest(createTaskDescription(new TeeEnclaveConfiguration()));

        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> teeSecretsService.getAppTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.APP_COMPUTE_INVALID_ENCLAVE_CONFIG, exception.getError());
    }

    @Test
    void shouldAddMultipleRequesterSecrets() {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        String requesterAddress = request.getTaskDescription().getRequester();

        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_2, REQUESTER_SECRET_VALUE_2);
        SecretEnclaveBase enclaveBase = assertDoesNotThrow(() -> teeSecretsService.getAppTokens(request));
        verify(teeTaskComputeSecretService, times(2))
                .getSecret(eq(OnChainObjectType.APPLICATION), eq(""), eq(SecretOwnerRole.REQUESTER), any(), any());
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER,
                requesterAddress, REQUESTER_SECRET_KEY_1);
        verify(teeTaskComputeSecretService).getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER,
                requesterAddress, REQUESTER_SECRET_KEY_2);
        assertThat(enclaveBase.getEnvironment()).containsAllEntriesOf(
                Map.of(
                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_1,
                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "2", REQUESTER_SECRET_VALUE_2));
    }

    @Test
    void shouldFilterRequesterSecretIndexLowerThanZero() {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        String requesterAddress = request.getTaskDescription().getRequester();

        request.getTaskDescription()
                .setSecrets(Map.of("1", REQUESTER_SECRET_KEY_1, "-1", "out-of-bound-requester-secret"));
        addRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        SecretEnclaveBase enclaveBase = assertDoesNotThrow(() -> teeSecretsService.getAppTokens(request));
        verify(teeTaskComputeSecretService).getSecret(eq(OnChainObjectType.APPLICATION), eq(""),
                eq(SecretOwnerRole.REQUESTER), any(), any());
        assertThat(enclaveBase.getEnvironment()).containsAllEntriesOf(
                Map.of(IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_1));
    }
    // endregion

    // region getPostComputeTokens
    @Test
    void shouldGetPostComputeTokens() throws Exception {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        String requesterAddress = request.getTaskDescription().getRequester();

        Secret publicKeySecret = new Secret("address", ENCRYPTION_PUBLIC_KEY);
        when(postComputeProperties.getFingerprint())
                .thenReturn(POST_COMPUTE_FINGERPRINT);
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

        SecretEnclaveBase enclaveBase = teeSecretsService.getPostComputeTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("post-compute");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(POST_COMPUTE_FINGERPRINT);
        Map<String, Object> expectedTokens = new HashMap<>();
        // encryption tokens
        expectedTokens.put("RESULT_ENCRYPTION", "yes");
        expectedTokens.put("RESULT_ENCRYPTION_PUBLIC_KEY", ENCRYPTION_PUBLIC_KEY);
        // storage tokens
        expectedTokens.put("RESULT_STORAGE_CALLBACK", "no");
        expectedTokens.put("RESULT_STORAGE_PROVIDER", STORAGE_PROVIDER);
        expectedTokens.put("RESULT_STORAGE_PROXY", STORAGE_PROXY);
        expectedTokens.put("RESULT_STORAGE_TOKEN", STORAGE_TOKEN);
        // sign tokens
        expectedTokens.put("RESULT_TASK_ID", TASK_ID);
        expectedTokens.put("RESULT_SIGN_WORKER_ADDRESS", WORKER_ADDRESS);
        expectedTokens.put("RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY", challenge.getCredentials().getPrivateKey());

        assertThat(enclaveBase.getEnvironment()).containsExactlyEntriesOf(expectedTokens);
    }

    @Test
    void shouldNotGetPostComputeTokensSinceTaskDescriptionMissing() {
        TeeSessionRequest request = TeeSessionRequest.builder().build();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeTokens(request));
        assertEquals(TeeSessionGenerationError.NO_TASK_DESCRIPTION, exception.getError());
        assertEquals("Task description must not be null", exception.getMessage());
    }
    // endregion

    // region getPostComputeEncryptionTokens
    @Test
    void shouldGetPostComputeStorageTokensWithCallback() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        sessionRequest.getTaskDescription().setCallback("callback");

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_STORAGE_CALLBACK", "yes",
                                "RESULT_STORAGE_PROVIDER", "",
                                "RESULT_STORAGE_PROXY", "",
                                "RESULT_STORAGE_TOKEN", ""));
    }

    @Test
    void shouldGetPostComputeStorageTokensOnIpfs() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();

        final String secretValue = "Secret value";
        when(web2SecretsService.getSecret(taskDescription.getRequester(),
                ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN, true))
                .thenReturn(Optional.of(new Secret(null, secretValue)));

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_STORAGE_CALLBACK", "no",
                                "RESULT_STORAGE_PROVIDER", STORAGE_PROVIDER,
                                "RESULT_STORAGE_PROXY", STORAGE_PROXY,
                                "RESULT_STORAGE_TOKEN", secretValue));
    }

    @Test
    void shouldGetPostComputeStorageTokensOnDropbox() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();
        taskDescription.setResultStorageProvider(DealParams.DROPBOX_RESULT_STORAGE_PROVIDER);

        final String secretValue = "Secret value";
        when(web2SecretsService.getSecret(taskDescription.getRequester(),
                ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN, true))
                .thenReturn(Optional.of(new Secret(null, secretValue)));

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_STORAGE_CALLBACK", "no",
                                "RESULT_STORAGE_PROVIDER", "dropbox",
                                "RESULT_STORAGE_PROXY", STORAGE_PROXY,
                                "RESULT_STORAGE_TOKEN", secretValue));
    }

    @Test
    void shouldNotGetPostComputeStorageTokensSinceNoSecret() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();

        when(web2SecretsService.getSecret(taskDescription.getRequester(),
                ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN, true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest));

        assertThat(exception.getError()).isEqualTo(TeeSessionGenerationError.POST_COMPUTE_GET_STORAGE_TOKENS_FAILED);
        assertThat(exception.getMessage())
                .isEqualTo("Empty requester storage token - taskId: " + taskDescription.getChainTaskId());
    }

    @Test
    void shouldGetPostComputeSignTokens() throws GeneralSecurityException {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();
        final String taskId = taskDescription.getChainTaskId();
        final EthereumCredentials credentials = EthereumCredentials.generate();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.of(TeeChallenge.builder().credentials(credentials).build()));

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_TASK_ID", taskId,
                                "RESULT_SIGN_WORKER_ADDRESS", sessionRequest.getWorkerAddress(),
                                "RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY", credentials.getPrivateKey()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "" })
    void shouldNotGetPostComputeSignTokensSinceNoWorkerAddress(String emptyWorkerAddress) {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();
        sessionRequest.setWorkerAddress(emptyWorkerAddress);

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

        assertThat(exception.getError())
                .isEqualTo(TeeSessionGenerationError.POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_WORKER_ADDRESS);
        assertThat(exception.getMessage()).isEqualTo("Empty worker address - taskId: " + taskId);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "" })
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveChallenge(String emptyEnclaveChallenge) {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();
        sessionRequest.setEnclaveChallenge(emptyEnclaveChallenge);

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

        assertThat(exception.getError()).isEqualTo(
                TeeSessionGenerationError.POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_PUBLIC_ENCLAVE_CHALLENGE);
        assertThat(exception.getMessage()).isEqualTo("Empty public enclave challenge - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoTeeChallenge() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

        assertThat(exception.getError())
                .isEqualTo(TeeSessionGenerationError.POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CHALLENGE);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge  - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveCredentials() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional.of(TeeChallenge.builder().credentials(null).build()));

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

        assertThat(exception.getError())
                .isEqualTo(TeeSessionGenerationError.POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge credentials - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveCredentialsPrivateKey() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig));
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        when(teeChallengeService.getOrCreate(taskId, true))
                .thenReturn(Optional
                        .of(TeeChallenge.builder().credentials(new EthereumCredentials("", "", false, "")).build()));

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

        assertThat(exception.getError())
                .isEqualTo(TeeSessionGenerationError.POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS);
        assertThat(exception.getMessage()).isEqualTo("Empty TEE challenge credentials - taskId: " + taskId);
    }

    @Test
    void shouldGetPostComputeEncryptionTokensWithEncryption() {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        Secret publicKeySecret = new Secret("address", ENCRYPTION_PUBLIC_KEY);
        when(web2SecretsService.getSecret(
                request.getTaskDescription().getBeneficiary(),
                ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.of(publicKeySecret));

        final Map<String, String> encryptionTokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeEncryptionTokens(request));
        assertThat(encryptionTokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_ENCRYPTION", "yes",
                                "RESULT_ENCRYPTION_PUBLIC_KEY", ENCRYPTION_PUBLIC_KEY));
    }

    @Test
    void shouldGetPostComputeEncryptionTokensWithoutEncryption() {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));
        request.getTaskDescription().setResultEncryption(false);

        final Map<String, String> encryptionTokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeEncryptionTokens(request));
        assertThat(encryptionTokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_ENCRYPTION", "no",
                                "RESULT_ENCRYPTION_PUBLIC_KEY", ""));
    }

    @Test
    void shouldNotGetPostComputeEncryptionTokensSinceEmptyBeneficiaryKey() {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig));

        when(web2SecretsService.getSecret(
                request.getTaskDescription().getBeneficiary(),
                ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true))
                .thenReturn(Optional.empty());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeEncryptionTokens(request));
        assertEquals(TeeSessionGenerationError.POST_COMPUTE_GET_ENCRYPTION_TOKENS_FAILED_EMPTY_BENEFICIARY_KEY,
                exception.getError());
        assertEquals("Empty beneficiary encryption key - taskId: taskId", exception.getMessage());
    }

    // endregion

    // region utils
    private void addApplicationDeveloperSecret(String appAddress) {
        TeeTaskComputeSecret applicationDeveloperSecret = getApplicationDeveloperSecret(appAddress);
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER, "", APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(Optional.of(applicationDeveloperSecret));
    }

    private void addRequesterSecret(String requesterAddress, String secretKey, String secretValue) {
        TeeTaskComputeSecret requesterSecret = getRequesterSecret(requesterAddress, secretKey, secretValue);
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER,
                requesterAddress, secretKey))
                .thenReturn(Optional.of(requesterSecret));
    }
    // endregion

}