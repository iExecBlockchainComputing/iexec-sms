/*
 * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.base;

import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.commons.poco.chain.DealParams;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.secret.compute.TeeTaskComputeSecret;
import com.iexec.sms.secret.compute.TeeTaskComputeSecretHeader;
import com.iexec.sms.secret.compute.TeeTaskComputeSecretService;
import com.iexec.sms.secret.web2.Web2Secret;
import com.iexec.sms.secret.web2.Web2SecretService;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.EthereumCredentials;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.iexec.sms.secret.ReservedSecretKeyName.*;
import static com.iexec.sms.tee.session.TeeSessionTestUtils.*;
import static com.iexec.sms.tee.session.base.SecretSessionBaseService.EMPTY_STRING_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecretSessionBaseServiceTests {

    private static final TeeEnclaveConfiguration enclaveConfig = TeeEnclaveConfiguration.builder()
            .framework(TeeFramework.SCONE)// any would be fine
            .entrypoint(APP_ENTRYPOINT)
            .fingerprint(APP_FINGERPRINT)
            .heapSize(1)
            .build();

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
    private Web3SecretService web3SecretService;
    @Mock
    private Web2SecretService web2SecretService;
    @Mock
    private TeeChallengeService teeChallengeService;
    @Mock
    private TeeServicesProperties teeServicesConfig;
    @Mock
    private TeeTaskComputeSecretService teeTaskComputeSecretService;

    @InjectMocks
    private SecretSessionBaseService teeSecretsService;

    @Captor
    private ArgumentCaptor<List<TeeTaskComputeSecretHeader>> teeTaskComputeSecretIds;

    // region getSecretsTokens
    @Test
    void shouldGetSecretsTokens() throws Exception {
        TaskDescription taskDescription = createTaskDescription(enclaveConfig).build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        String beneficiary = request.getTaskDescription().getBeneficiary();

        when(teeServicesConfig.getPreComputeProperties()).thenReturn(preComputeProperties);
        when(teeServicesConfig.getPostComputeProperties()).thenReturn(postComputeProperties);

        // pre
        when(web3SecretService.getDecryptedValue(DATASET_ADDRESS))
                .thenReturn(Optional.of(DATASET_KEY));
        // post
        final Web2Secret resultEncryption = new Web2Secret(beneficiary, IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY, ENCRYPTION_PUBLIC_KEY);
        final Web2Secret requesterStorageToken = new Web2Secret(taskDescription.getRequester(), IEXEC_RESULT_IEXEC_IPFS_TOKEN, STORAGE_TOKEN);
        final Web2Secret workerStorageToken = new Web2Secret(WORKER_ADDRESS, IEXEC_RESULT_IEXEC_IPFS_TOKEN, STORAGE_TOKEN);
        final Web2Secret resultProxyUrl = new Web2Secret(taskDescription.getWorkerpoolOwner(), IEXEC_RESULT_IEXEC_RESULT_PROXY_URL, "");
        when(web2SecretService.getSecretsForTeeSession(List.of(resultEncryption.getHeader(), requesterStorageToken.getHeader(), workerStorageToken.getHeader(), resultProxyUrl.getHeader())))
                .thenReturn(List.of(resultEncryption, requesterStorageToken, workerStorageToken, resultProxyUrl));
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
        TaskDescription taskDescription = createTaskDescription(enclaveConfig).build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        when(teeServicesConfig.getPreComputeProperties()).thenReturn(preComputeProperties);
        when(web3SecretService.getDecryptedValue(DATASET_ADDRESS))
                .thenReturn(Optional.of(DATASET_KEY));

        SecretEnclaveBase enclaveBase = teeSecretsService.getPreComputeTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("pre-compute");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(PRE_COMPUTE_FINGERPRINT);
        Map<String, Object> expectedTokens = new HashMap<>();
        expectedTokens.put("IEXEC_TASK_ID", TASK_ID);
        expectedTokens.put("IEXEC_PRE_COMPUTE_OUT", "/iexec_in");
        expectedTokens.put("IS_DATASET_REQUIRED", true);
        expectedTokens.put("IEXEC_DATASET_KEY", DATASET_KEY);
        expectedTokens.put("IEXEC_DATASET_URL", DATASET_URL);
        expectedTokens.put("IEXEC_DATASET_FILENAME", DATASET_ADDRESS);
        expectedTokens.put("IEXEC_DATASET_CHECKSUM", DATASET_CHECKSUM);
        expectedTokens.put("IEXEC_INPUT_FILES_FOLDER", "/iexec_in");
        expectedTokens.put("IEXEC_INPUT_FILES_NUMBER", "2");
        expectedTokens.put("IEXEC_INPUT_FILE_URL_1", INPUT_FILE_URL_1);
        expectedTokens.put("IEXEC_INPUT_FILE_URL_2", INPUT_FILE_URL_2);
        assertThat(enclaveBase.getEnvironment()).containsExactlyInAnyOrderEntriesOf(expectedTokens);
    }

    @Test
    void shouldGetPreComputeTokensWithoutDataset() throws Exception {
        when(teeServicesConfig.getPreComputeProperties()).thenReturn(preComputeProperties);
        TeeSessionRequest request = TeeSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(TaskDescription.builder()
                        .chainTaskId(TASK_ID)
                        .inputFiles(List.of(INPUT_FILE_URL_1, INPUT_FILE_URL_2))
                        .build())
                .build();

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
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig).build());

        String appAddress = request.getTaskDescription().getAppAddress();
        String requesterAddress = request.getTaskDescription().getRequester();

        final TeeTaskComputeSecret applicationSecret = getApplicationDeveloperSecret(appAddress);
        final TeeTaskComputeSecret requesterSecret1 = getRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        final TeeTaskComputeSecret requesterSecret2 = getRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_2, REQUESTER_SECRET_VALUE_2);
        when(teeTaskComputeSecretService.getSecretsForTeeSession(teeTaskComputeSecretIds.capture()))
                .thenReturn(List.of(applicationSecret, requesterSecret1, requesterSecret2));

        SecretEnclaveBase enclaveBase = teeSecretsService.getAppTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("app");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(APP_FINGERPRINT);
        Map<String, Object> expectedTokens = new HashMap<>();
        expectedTokens.put("IEXEC_TASK_ID", TASK_ID);
        expectedTokens.put("IEXEC_IN", "/iexec_in");
        expectedTokens.put("IEXEC_OUT", "/iexec_out");
        expectedTokens.put("IEXEC_DATASET_ADDRESS", DATASET_ADDRESS);
        expectedTokens.put("IEXEC_DATASET_FILENAME", DATASET_ADDRESS);
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
        assertThat(teeTaskComputeSecretIds.getValue()).containsExactlyInAnyOrder(
                applicationSecret.getHeader(), requesterSecret1.getHeader(), requesterSecret2.getHeader());
        verify(teeTaskComputeSecretService).getSecretsForTeeSession(anyCollection());
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

        TeeTaskComputeSecret applicationSecret = getApplicationDeveloperSecret(appAddress);
        when(teeTaskComputeSecretService.getSecretsForTeeSession(List.of(applicationSecret.getHeader())))
                .thenReturn(List.of());

        SecretEnclaveBase enclaveBase = teeSecretsService.getAppTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("app");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(APP_FINGERPRINT);
        Map<String, Object> expectedTokens = new HashMap<>();
        expectedTokens.put("IEXEC_TASK_ID", TASK_ID);
        expectedTokens.put("IEXEC_IN", "/iexec_in");
        expectedTokens.put("IEXEC_OUT", "/iexec_out");
        expectedTokens.put("IEXEC_DATASET_ADDRESS", DATASET_ADDRESS);
        expectedTokens.put("IEXEC_DATASET_FILENAME", DATASET_ADDRESS);
        expectedTokens.put("IEXEC_BOT_SIZE", "1");
        expectedTokens.put("IEXEC_BOT_FIRST_INDEX", "0");
        expectedTokens.put("IEXEC_BOT_TASK_INDEX", "0");
        expectedTokens.put("IEXEC_INPUT_FILES_FOLDER", "/iexec_in");
        expectedTokens.put("IEXEC_INPUT_FILES_NUMBER", "2");
        expectedTokens.put("IEXEC_INPUT_FILE_NAME_1", INPUT_FILE_NAME_1);
        expectedTokens.put("IEXEC_INPUT_FILE_NAME_2", INPUT_FILE_NAME_2);

        assertThat(enclaveBase.getEnvironment()).containsExactlyInAnyOrderEntriesOf(expectedTokens);
        verify(teeTaskComputeSecretService).getSecretsForTeeSession(anyCollection());
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
        final TaskDescription taskDescription = createTaskDescription(TeeEnclaveConfiguration.builder().build()).build();
        TeeSessionRequest request = createSessionRequest(taskDescription);

        TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class,
                () -> teeSecretsService.getAppTokens(request));
        Assertions.assertEquals(TeeSessionGenerationError.APP_COMPUTE_INVALID_ENCLAVE_CONFIG, exception.getError());
    }

    @Test
    void shouldAddMultipleRequesterSecrets() {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig).build());
        String requesterAddress = request.getTaskDescription().getRequester();

        final TeeTaskComputeSecret applicationSecret = getApplicationDeveloperSecret(request.getTaskDescription().getAppAddress());
        final TeeTaskComputeSecret requesterSecret1 = getRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        final TeeTaskComputeSecret requesterSecret2 = getRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_2, REQUESTER_SECRET_VALUE_2);
        when(teeTaskComputeSecretService.getSecretsForTeeSession(teeTaskComputeSecretIds.capture()))
                .thenReturn(List.of(requesterSecret1, requesterSecret2));
        SecretEnclaveBase enclaveBase = assertDoesNotThrow(() -> teeSecretsService.getAppTokens(request));

        verify(teeTaskComputeSecretService).getSecretsForTeeSession(anyCollection());
        assertThat(teeTaskComputeSecretIds.getValue()).containsExactlyInAnyOrder(
                applicationSecret.getHeader(), requesterSecret1.getHeader(), requesterSecret2.getHeader());
        assertThat(enclaveBase.getEnvironment()).containsAllEntriesOf(
                Map.of(
                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_1,
                        IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "2", REQUESTER_SECRET_VALUE_2));
    }

    @Test
    void shouldFilterRequesterSecretIndexLowerThanZero() {
        final TaskDescription taskDescription = createTaskDescription(enclaveConfig)
                .secrets(Map.of("1", REQUESTER_SECRET_KEY_1, "-1", "out-of-bound-requester-secret"))
                .build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        String requesterAddress = request.getTaskDescription().getRequester();

        final TeeTaskComputeSecret applicationSecret = getApplicationDeveloperSecret(request.getTaskDescription().getAppAddress());
        final TeeTaskComputeSecret requesterSecret = getRequesterSecret(requesterAddress, REQUESTER_SECRET_KEY_1, REQUESTER_SECRET_VALUE_1);
        when(teeTaskComputeSecretService.getSecretsForTeeSession(List.of(applicationSecret.getHeader(), requesterSecret.getHeader())))
                .thenReturn(List.of(requesterSecret));
        SecretEnclaveBase enclaveBase = assertDoesNotThrow(() -> teeSecretsService.getAppTokens(request));
        verify(teeTaskComputeSecretService).getSecretsForTeeSession(anyCollection());
        assertThat(enclaveBase.getEnvironment()).containsAllEntriesOf(
                Map.of(IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + "1", REQUESTER_SECRET_VALUE_1));
    }
    // endregion

    // region getPostComputeTokens
    @Test
    void shouldGetPostComputeTokens() throws Exception {
        final TaskDescription taskDescription = createTaskDescription(enclaveConfig).build();
        final TeeSessionRequest request = createSessionRequest(taskDescription);

        final String beneficiary = request.getTaskDescription().getBeneficiary();
        final Web2Secret resultEncryption = new Web2Secret(beneficiary, IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY, ENCRYPTION_PUBLIC_KEY);
        final Web2Secret requesterStorageToken = new Web2Secret(taskDescription.getRequester(), IEXEC_RESULT_IEXEC_IPFS_TOKEN, STORAGE_TOKEN);
        final Web2Secret workerStorageToken = new Web2Secret(WORKER_ADDRESS, IEXEC_RESULT_IEXEC_IPFS_TOKEN, STORAGE_TOKEN);
        final Web2Secret resultProxyUrl = new Web2Secret(taskDescription.getWorkerpoolOwner(), IEXEC_RESULT_IEXEC_RESULT_PROXY_URL, "");
        when(teeServicesConfig.getPostComputeProperties()).thenReturn(postComputeProperties);
        when(web2SecretService.getSecretsForTeeSession(List.of(resultEncryption.getHeader(), requesterStorageToken.getHeader(), workerStorageToken.getHeader(), resultProxyUrl.getHeader())))
                .thenReturn(List.of(resultEncryption, requesterStorageToken, workerStorageToken, resultProxyUrl));

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
    void shouldGetPostComputeTokensForDropbox() throws TeeSessionGenerationException, GeneralSecurityException {
        when(teeServicesConfig.getPostComputeProperties()).thenReturn(postComputeProperties);
        final TaskDescription taskDescription = createTaskDescription(enclaveConfig)
                .resultStorageProvider(DealParams.DROPBOX_RESULT_STORAGE_PROVIDER)
                .build();
        final TeeSessionRequest request = createSessionRequest(taskDescription);

        final String beneficiary = request.getTaskDescription().getBeneficiary();
        final Web2Secret resultEncryption = new Web2Secret(beneficiary, IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY, ENCRYPTION_PUBLIC_KEY);
        final Web2Secret dropboxToken = new Web2Secret(taskDescription.getRequester(), IEXEC_RESULT_DROPBOX_TOKEN, "Secret value");
        when(web2SecretService.getSecretsForTeeSession(List.of(resultEncryption.getHeader(), dropboxToken.getHeader())))
                .thenReturn(List.of(resultEncryption, dropboxToken));
        TeeChallenge challenge = TeeChallenge.builder()
                .credentials(EthereumCredentials.generate())
                .build();
        when(teeChallengeService.getOrCreate(TASK_ID, true))
                .thenReturn(Optional.of(challenge));

        SecretEnclaveBase enclaveBase = teeSecretsService.getPostComputeTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("post-compute");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(POST_COMPUTE_FINGERPRINT);
    }

    @Test
    void shouldGetPostComputeTokensWithCallback() throws TeeSessionGenerationException, GeneralSecurityException {
        when(teeServicesConfig.getPostComputeProperties()).thenReturn(postComputeProperties);
        final TaskDescription taskDescription = createTaskDescription(enclaveConfig)
                .callback("callback")
                .build();
        final TeeSessionRequest request = createSessionRequest(taskDescription);

        final String beneficiary = request.getTaskDescription().getBeneficiary();
        final Web2Secret resultEncryption = new Web2Secret(beneficiary, IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY, ENCRYPTION_PUBLIC_KEY);
        final Web2Secret requesterStorageToken = new Web2Secret(taskDescription.getRequester(), IEXEC_RESULT_IEXEC_IPFS_TOKEN, STORAGE_TOKEN);
        final Web2Secret workerStorageToken = new Web2Secret(WORKER_ADDRESS, IEXEC_RESULT_IEXEC_IPFS_TOKEN, STORAGE_TOKEN);
        final Web2Secret resultProxyUrl = new Web2Secret(taskDescription.getWorkerpoolOwner(), IEXEC_RESULT_IEXEC_RESULT_PROXY_URL, "");
        when(web2SecretService.getSecretsForTeeSession(List.of(resultEncryption.getHeader(), requesterStorageToken.getHeader(), workerStorageToken.getHeader(), resultProxyUrl.getHeader())))
                .thenReturn(List.of(resultEncryption, workerStorageToken, resultProxyUrl));
        TeeChallenge challenge = TeeChallenge.builder()
                .credentials(EthereumCredentials.generate())
                .build();
        when(teeChallengeService.getOrCreate(TASK_ID, true))
                .thenReturn(Optional.of(challenge));

        SecretEnclaveBase enclaveBase = teeSecretsService.getPostComputeTokens(request);
        assertThat(enclaveBase.getName()).isEqualTo("post-compute");
        assertThat(enclaveBase.getMrenclave()).isEqualTo(POST_COMPUTE_FINGERPRINT);
    }
    // endregion

    // region getPostComputeStorageTokens
    @Test
    void shouldGetPostComputeStorageTokensWithCallback() {
        final TaskDescription taskDescription = createTaskDescription(enclaveConfig)
                .callback("callback")
                .build();
        final TeeSessionRequest sessionRequest = createSessionRequest(taskDescription);

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest, "", ""));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_STORAGE_CALLBACK", "yes",
                                "RESULT_STORAGE_PROVIDER", EMPTY_STRING_VALUE,
                                "RESULT_STORAGE_PROXY", EMPTY_STRING_VALUE,
                                "RESULT_STORAGE_TOKEN", EMPTY_STRING_VALUE));
    }

    @Test
    void shouldGetPostComputeStorageTokensOnIpfsWithStorageToken() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig).build());

        final String storageToken = "storageToken";

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest, storageToken, ""));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_STORAGE_CALLBACK", "no",
                                "RESULT_STORAGE_PROVIDER", DealParams.IPFS_RESULT_STORAGE_PROVIDER,
                                "RESULT_STORAGE_PROXY", STORAGE_PROXY,
                                "RESULT_STORAGE_TOKEN", storageToken));
    }

    @Test
    void shouldGetPostComputeStorageTokensOnDropbox() {
        final TaskDescription taskDescription = createTaskDescription(enclaveConfig)
                .resultStorageProvider(DealParams.DROPBOX_RESULT_STORAGE_PROVIDER)
                .resultStorageProxy("")
                .build();
        final TeeSessionRequest sessionRequest = createSessionRequest(taskDescription);

        final String secretValue = "Secret value";

        final Map<String, String> tokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest, secretValue, ""));

        assertThat(tokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_STORAGE_CALLBACK", "no",
                                "RESULT_STORAGE_PROVIDER", DealParams.DROPBOX_RESULT_STORAGE_PROVIDER,
                                "RESULT_STORAGE_PROXY", EMPTY_STRING_VALUE,
                                "RESULT_STORAGE_TOKEN", secretValue));
    }

    @Test
    void shouldNotGetPostComputeStorageTokensSinceNoSecret() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig).build());
        final TaskDescription taskDescription = sessionRequest.getTaskDescription();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeStorageTokens(sessionRequest, "", ""));

        assertThat(exception.getError()).isEqualTo(TeeSessionGenerationError.POST_COMPUTE_GET_STORAGE_TOKENS_FAILED);
        assertThat(exception.getMessage())
                .isEqualTo("Empty requester storage token - taskId: " + taskDescription.getChainTaskId());
    }

    // region getPostComputeSignTokens
    @Test
    void shouldGetPostComputeSignTokens() throws GeneralSecurityException {
        final TaskDescription taskDescription = createTaskDescription(enclaveConfig).build();
        final TeeSessionRequest sessionRequest = createSessionRequest(taskDescription);
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
    @ValueSource(strings = {""})
    void shouldNotGetPostComputeSignTokensSinceNoWorkerAddress(String emptyWorkerAddress) {
        final TeeSessionRequest sessionRequest = createSessionRequestBuilder(createTaskDescription(enclaveConfig).build())
                .workerAddress(emptyWorkerAddress)
                .build();
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

        assertThat(exception.getError())
                .isEqualTo(TeeSessionGenerationError.POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_WORKER_ADDRESS);
        assertThat(exception.getMessage()).isEqualTo("Empty worker address - taskId: " + taskId);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldNotGetPostComputeSignTokensSinceNoEnclaveChallenge(String emptyEnclaveChallenge) {
        final TeeSessionRequest sessionRequest = createSessionRequestBuilder(createTaskDescription(enclaveConfig).build())
                .enclaveChallenge(emptyEnclaveChallenge)
                .build();
        final String taskId = sessionRequest.getTaskDescription().getChainTaskId();

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeSignTokens(sessionRequest));

        assertThat(exception.getError()).isEqualTo(
                TeeSessionGenerationError.POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_PUBLIC_ENCLAVE_CHALLENGE);
        assertThat(exception.getMessage()).isEqualTo("Empty public enclave challenge - taskId: " + taskId);
    }

    @Test
    void shouldNotGetPostComputeSignTokensSinceNoTeeChallenge() {
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig).build());
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
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig).build());
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
        final TeeSessionRequest sessionRequest = createSessionRequest(createTaskDescription(enclaveConfig).build());
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
    // endregion

    // region getPostcomputeEncryptionTokens
    @Test
    void shouldGetPostComputeEncryptionTokensWithEncryption() {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig).build());

        final Map<String, String> encryptionTokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeEncryptionTokens(request, ENCRYPTION_PUBLIC_KEY));
        assertThat(encryptionTokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_ENCRYPTION", "yes",
                                "RESULT_ENCRYPTION_PUBLIC_KEY", ENCRYPTION_PUBLIC_KEY));
    }

    @Test
    void shouldGetPostComputeEncryptionTokensWithoutEncryption() {
        final TaskDescription taskDescription = createTaskDescription(enclaveConfig)
                .isResultEncryption(false)
                .build();
        TeeSessionRequest request = createSessionRequest(taskDescription);

        final Map<String, String> encryptionTokens = assertDoesNotThrow(
                () -> teeSecretsService.getPostComputeEncryptionTokens(request, ""));
        assertThat(encryptionTokens)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "RESULT_ENCRYPTION", "no",
                                "RESULT_ENCRYPTION_PUBLIC_KEY", ""));
    }

    @Test
    void shouldNotGetPostComputeEncryptionTokensSinceEmptyBeneficiaryKey() {
        TeeSessionRequest request = createSessionRequest(createTaskDescription(enclaveConfig).build());

        final TeeSessionGenerationException exception = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSecretsService.getPostComputeEncryptionTokens(request, ""));
        assertEquals(TeeSessionGenerationError.POST_COMPUTE_GET_ENCRYPTION_TOKENS_FAILED_EMPTY_BENEFICIARY_KEY,
                exception.getError());
        assertEquals("Empty beneficiary encryption key - taskId: taskId", exception.getMessage());
    }
    // endregion

}
