/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session;

import com.iexec.common.precompute.PreComputeUtils;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.sms.secret.compute.OnChainObjectType;
import com.iexec.sms.secret.compute.SecretOwnerRole;
import com.iexec.sms.secret.compute.TeeTaskComputeSecret;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.iexec.common.worker.result.ResultUtils.*;
import static com.iexec.sms.Web3jUtils.createEthereumAddress;
import static com.iexec.sms.tee.session.base.SecretSessionBaseService.*;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TeeSessionTestUtils {
    public static final String TASK_ID = "taskId";
    public static final String SESSION_ID = "sessionId";
    public static final String WORKER_ADDRESS = "workerAddress";
    public static final String ENCLAVE_CHALLENGE = "enclaveChallenge";
    // pre-compute
    public static final String PRE_COMPUTE_FINGERPRINT = "mrEnclave1";
    public static final String PRE_COMPUTE_ENTRYPOINT = "entrypoint1";
    public static final String DATASET_ADDRESS = "0xDatasetAddress";
    public static final String DATASET_NAME = "datasetName";
    public static final String DATASET_CHECKSUM = "datasetChecksum";
    public static final String DATASET_URL = "http://datasetUrl"; // 0x687474703a2f2f646174617365742d75726c in hex
    // keys with leading/trailing \n should not break the workflow
    public static final String DATASET_KEY = "\ndatasetKey\n";
    // app
    public static final String APP_DEVELOPER_SECRET_INDEX = "1";
    public static final String APP_DEVELOPER_SECRET_VALUE = "appDeveloperSecretValue";
    public static final String REQUESTER_SECRET_KEY_1 = "requesterSecretKey1";
    public static final String REQUESTER_SECRET_VALUE_1 = "requesterSecretValue1";
    public static final String REQUESTER_SECRET_KEY_2 = "requesterSecretKey2";
    public static final String REQUESTER_SECRET_VALUE_2 = "requesterSecretValue2";
    public static final String APP_URI = "appUri";
    public static final String APP_FINGERPRINT = "01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b";
    public static final String APP_ENTRYPOINT = "appEntrypoint";
    public static final String ARGS = "args";
    public static final String IEXEC_APP_DEVELOPER_SECRET_1 = "IEXEC_APP_DEVELOPER_SECRET_1";
    // post-compute
    public static final String POST_COMPUTE_FINGERPRINT = "mrEnclave3";
    public static final String POST_COMPUTE_ENTRYPOINT = "entrypoint3";
    public static final String STORAGE_PROVIDER = "ipfs";
    public static final String STORAGE_PROXY = "storageProxy";
    public static final String STORAGE_TOKEN = "storageToken";
    public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
    public static final String TEE_CHALLENGE_PRIVATE_KEY = "teeChallengePrivateKey";
    // input files
    public static final String INPUT_FILE_URL_1 = "http://host/file1";
    public static final String INPUT_FILE_NAME_1 = "file1";
    public static final String INPUT_FILE_URL_2 = "http://host/file2";
    public static final String INPUT_FILE_NAME_2 = "file2";

    //region utils
    public static TeeTaskComputeSecret getApplicationDeveloperSecret(String appAddress) {
        return TeeTaskComputeSecret.builder()
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .onChainObjectAddress(appAddress)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .key(APP_DEVELOPER_SECRET_INDEX)
                .value(APP_DEVELOPER_SECRET_VALUE)
                .build();
    }

    public static TeeTaskComputeSecret getRequesterSecret(String requesterAddress, String secretKey, String secretValue) {
        return TeeTaskComputeSecret.builder()
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .onChainObjectAddress("")
                .secretOwnerRole(SecretOwnerRole.REQUESTER)
                .fixedSecretOwner(requesterAddress)
                .key(secretKey)
                .value(secretValue)
                .build();
    }

    public static TeeSessionRequest createSessionRequest(TaskDescription taskDescription) {
        return TeeSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(taskDescription)
                .build();
    }

    public static TaskDescription createTaskDescription(TeeEnclaveConfiguration enclaveConfig) {
        String appAddress = createEthereumAddress();
        String requesterAddress = createEthereumAddress();
        return TaskDescription.builder()
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
                .secrets(Map.of("1", REQUESTER_SECRET_KEY_1, "2", REQUESTER_SECRET_KEY_2))
                .botSize(1)
                .botFirstIndex(0)
                .botIndex(0)
                .build();
    }

    public static Map<String, Object> getPreComputeTokens() {
        return Map.of(
                PRE_COMPUTE_MRENCLAVE, PRE_COMPUTE_FINGERPRINT,
                PreComputeUtils.IS_DATASET_REQUIRED, true,
                PreComputeUtils.IEXEC_DATASET_KEY, DATASET_KEY.trim(),
                INPUT_FILE_URLS, Map.of(
                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "1", INPUT_FILE_URL_1,
                        IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX + "2", INPUT_FILE_URL_2));
    }

    public static Map<String, Object> getAppTokens() {
        return Map.of(
            APP_MRENCLAVE, APP_FINGERPRINT,
            SecretSessionBaseService.INPUT_FILE_NAMES, Map.of(
                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "1", INPUT_FILE_NAME_1,
                        IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX + "2", INPUT_FILE_NAME_2));
    }

    public static Map<String, Object> getPostComputeTokens() {
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

    public static void assertRecursively(Object expected, Object actual) {
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
                final Object expectedObject = expectedMap.get(key);
                final Object actualObject = actualMap.get(key);
                log.info("Checking expected map contains valid '{}' key [expected value:{}, actual value:{}]", key, expectedObject, actualObject);
                assertRecursively(expectedObject, actualObject);
            });
        }
    }
    //endregion
}
