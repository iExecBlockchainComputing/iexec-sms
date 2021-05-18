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

import com.iexec.common.precompute.PreComputeConfig;
import com.iexec.common.precompute.PreComputeUtils;
import com.iexec.common.sms.secret.ReservedSecretKeyName;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.utils.FileHelper;
import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.common.worker.result.ResultUtils;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.precompute.PreComputeConfigService;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.iexec.common.precompute.PreComputeUtils.IEXEC_DATASET_KEY;
import static com.iexec.common.precompute.PreComputeUtils.INPUT_FILE_URLS;
import static com.iexec.common.precompute.PreComputeUtils.IS_DATASET_REQUIRED;
import static com.iexec.common.worker.result.ResultUtils.*;
import static com.iexec.sms.tee.session.palaemon.PalaemonSessionService.*;
import static com.iexec.sms.tee.session.palaemon.PalaemonSessionService.INPUT_FILE_NAMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Slf4j
public class PalaemonSessionServiceTests {

    private static final String TEMPLATE_SESSION_FILE = "src/main/resources/palaemonTemplate.vm";
    private static final String EXPECTED_SESSION_FILE = "src/test/resources/tee-session.yml";

    private static final String TASK_ID = "taskId";
    private static final String SESSION_ID = "sessionId";
    private static final String WORKER_ADDRESS = "workerAddress";
    private static final String ENCLAVE_CHALLENGE = "enclaveChallenge";
    private static final String REQUESTER = "requester";
    // pre-compute
    private static final String PRE_COMPUTE_FINGERPRINT = "mrEnclave1";
    private static final String DATASET_ADDRESS = "0xDatasetAddress";
    private static final String DATASET_NAME = "datasetName";
    private static final String DATASET_CHECKSUM = "datasetChecksum";
    private static final String DATASET_URL = "http://datasetUrl"; // 0x687474703a2f2f646174617365742d75726c in hex
    // keys with leading/trailing \n should not break the workflow
    private static final String DATASET_KEY = "\ndatasetKey\n";
    // app
    private static final String APP_URI = "appUri";
    private static final String APP_FINGERPRINT = "mrEnclave2|entryPoint";
    private static final String[] APP_FINGERPRINT_PARTS = APP_FINGERPRINT.split("\\|");
    private static final String ARGS = "args";
    // post-compute
    private static final String POST_COMPUTE_FINGERPRINT = "mrEnclave3";
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
    private IexecHubService iexecHubService;
    @Mock
    private Web3SecretService web3SecretService;
    @Mock
    private Web2SecretsService web2SecretsService;
    @Mock
    private TeeChallengeService teeChallengeService;
    @Mock
    private PreComputeConfigService preComputeConfigService;

    private PalaemonSessionService palaemonSessionService;

    @BeforeEach
    void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        // spy is needed to mock some internal calls of the tested
        // class when relevant
        palaemonSessionService = spy(new PalaemonSessionService(
                TEMPLATE_SESSION_FILE,
                web3SecretService,
                web2SecretsService,
                teeChallengeService,
                preComputeConfigService));
    }

    @Test
    public void shouldGetSessionYml() throws Exception {
        PalaemonSessionRequest request = createSessionRequest();
        doReturn(getPreComputeTokens()).when(palaemonSessionService)
                .getPreComputePalaemonTokens(request);
        doReturn(getAppTokens()).when(palaemonSessionService)
                .getAppPalaemonTokens(request);
        doReturn(getPostComputeTokens()).when(palaemonSessionService)
                .getPostComputePalaemonTokens(request);

        String actualYmlString = palaemonSessionService.getSessionYml(request);
        Map<String, Object> actualYmlMap = new Yaml().load(actualYmlString);
        String expectedYamlString = FileHelper.readFile(EXPECTED_SESSION_FILE);
        Map<String, Object> expectedYmlMap = new Yaml().load(expectedYamlString);
        assertRecursively(expectedYmlMap, actualYmlMap);
    }

    // pre-compute

    @Test
    public void shouldGetPreComputePalaemonTokens() throws Exception {
        PalaemonSessionRequest request = createSessionRequest();
        when(preComputeConfigService.getConfiguration()).thenReturn(
                        new PreComputeConfig("image",
                                PRE_COMPUTE_FINGERPRINT,
                                "heap"));
        Web3Secret secret = new Web3Secret(DATASET_ADDRESS, DATASET_KEY);
        when(web3SecretService.getSecret(DATASET_ADDRESS, true))
                .thenReturn(Optional.of(secret));

        Map<String, Object> tokens =
                palaemonSessionService.getPreComputePalaemonTokens(request);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens.get(PalaemonSessionService.PRE_COMPUTE_MRENCLAVE))
                .isEqualTo(PRE_COMPUTE_FINGERPRINT);
        assertThat(tokens.get(PreComputeUtils.IEXEC_DATASET_KEY))
                .isEqualTo(secret.getTrimmedValue());
        assertThat(tokens.get(PalaemonSessionService.INPUT_FILE_URLS))
                .isEqualTo(Map.of(
                    IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "1", INPUT_FILE_URL_1,
                    IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "2", INPUT_FILE_URL_2));
    }

    // app

    @Test
    public void shouldGetAppPalaemonTokens() throws Exception {
        PalaemonSessionRequest request = createSessionRequest();
        Map<String, Object> tokens =
                palaemonSessionService.getAppPalaemonTokens(request);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens.get(PalaemonSessionService.APP_MRENCLAVE))
                .isEqualTo(APP_FINGERPRINT_PARTS[0]);
        assertThat(tokens.get(PalaemonSessionService.APP_ARGS))
                .isEqualTo(APP_FINGERPRINT_PARTS[1] + " " + ARGS);
        assertThat(tokens.get(PalaemonSessionService.INPUT_FILE_NAMES))
                .isEqualTo(Map.of(
                    IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "1", "file1",
                    IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "2", "file2"));
    }

    // post-compute

    @Test
    public void shouldGetPostComputePalaemonTokens() throws Exception {
        PalaemonSessionRequest request = createSessionRequest();
        Secret publicKeySecret = new Secret("address", ENCRYPTION_PUBLIC_KEY);
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
                .credentials(new EthereumCredentials())
                .build();
        when(teeChallengeService.getOrCreate(TASK_ID, true))
                .thenReturn(Optional.of(challenge));


        Map<String, String> tokens =
                palaemonSessionService.getPostComputePalaemonTokens(request);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens.get(PalaemonSessionService.POST_COMPUTE_MRENCLAVE))
                .isEqualTo(POST_COMPUTE_FINGERPRINT);
        // encryption tokens
        assertThat(tokens.get(ResultUtils.RESULT_ENCRYPTION)).isEqualTo("yes") ;
        assertThat(tokens.get(ResultUtils.RESULT_ENCRYPTION_PUBLIC_KEY))
                .isEqualTo(ENCRYPTION_PUBLIC_KEY);
        // storage tokens
        assertThat(tokens.get(ResultUtils.RESULT_STORAGE_CALLBACK)).isEqualTo("no");
        assertThat(tokens.get(ResultUtils.RESULT_STORAGE_PROVIDER))
                .isEqualTo(STORAGE_PROVIDER);
        assertThat(tokens.get(ResultUtils.RESULT_STORAGE_PROXY))
                .isEqualTo(STORAGE_PROXY);
        assertThat(tokens.get(ResultUtils.RESULT_STORAGE_TOKEN))
                .isEqualTo(STORAGE_TOKEN);
        // sign tokens
        assertThat(tokens.get(ResultUtils.RESULT_TASK_ID)).isEqualTo(TASK_ID);
        assertThat(tokens.get(ResultUtils.RESULT_SIGN_WORKER_ADDRESS))
                .isEqualTo(WORKER_ADDRESS);
        assertThat(tokens.get(ResultUtils.RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY))
                .isEqualTo(challenge.getCredentials().getPrivateKey());
    }

    private PalaemonSessionRequest createSessionRequest() {
        return PalaemonSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(createTaskDescription())
                .build();
    }

    private TaskDescription createTaskDescription() {
        return TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .appUri(APP_URI)
                .appFingerprint(APP_FINGERPRINT)
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
                .teePostComputeFingerprint(POST_COMPUTE_FINGERPRINT)
                .teePostComputeImage(POST_COMPUTE_IMAGE)
                .botSize(1)
                .botFirstIndex(0)
                .botIndex(0)
                .build();
    }

    private Map<String, Object> getPreComputeTokens() {
        return Map.of(
                PRE_COMPUTE_MRENCLAVE, PRE_COMPUTE_FINGERPRINT,
                IS_DATASET_REQUIRED, true,
                IEXEC_DATASET_KEY, DATASET_KEY.trim(),
                INPUT_FILE_URLS, Map.of(
                    IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "1", INPUT_FILE_URL_1,
                    IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "2", INPUT_FILE_URL_2));
    }

    private Map<String, Object> getAppTokens() {
        return Map.of(
                APP_MRENCLAVE, APP_FINGERPRINT_PARTS[0],
                APP_ARGS, APP_FINGERPRINT_PARTS[1] + " " + ARGS,
                INPUT_FILE_NAMES, Map.of(
                    IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "1", INPUT_FILE_NAME_1,
                    IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "2", INPUT_FILE_NAME_2));
    }

    private Map<String, Object> getPostComputeTokens() {
        Map<String, Object> map = new HashMap<>();
        map.put(POST_COMPUTE_MRENCLAVE, POST_COMPUTE_FINGERPRINT);
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
}
