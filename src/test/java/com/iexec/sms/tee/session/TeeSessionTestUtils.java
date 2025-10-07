/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.utils.FileHashUtils;
import com.iexec.commons.poco.chain.DealParams;
import com.iexec.commons.poco.order.OrderTag;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.secret.compute.OnChainObjectType;
import com.iexec.sms.secret.compute.SecretOwnerRole;
import com.iexec.sms.secret.compute.TeeTaskComputeSecret;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static com.iexec.sms.Web3jUtils.createEthereumAddress;

@Slf4j
public class TeeSessionTestUtils {
    public static final String TEST_DEAL_ID = "dealId";
    public static final String TASK_ID = "taskId";
    public static final String SESSION_ID = "sessionId";
    public static final String WORKER_ADDRESS = "workerAddress";
    public static final String ENCLAVE_CHALLENGE = "enclaveChallenge";
    // pre-compute
    public static final String PRE_COMPUTE_FINGERPRINT = "mrEnclave1";
    public static final String PRE_COMPUTE_ENTRYPOINT = "entrypoint1";
    public static final String DATASET_ADDRESS = "0xDatasetAddress";
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
    // post-compute
    public static final String POST_COMPUTE_FINGERPRINT = "mrEnclave3";
    public static final String POST_COMPUTE_ENTRYPOINT = "entrypoint3";
    public static final String STORAGE_PROVIDER = "ipfs";
    public static final String STORAGE_PROXY = "storageProxy";
    public static final String STORAGE_TOKEN = "storageToken";
    public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
    // input files
    public static final String INPUT_FILE_URL_1 = "http://host/file1";
    public static final String INPUT_FILE_NAME_1 = FileHashUtils.createFileNameFromUri(INPUT_FILE_URL_1);
    public static final String INPUT_FILE_URL_2 = "http://host/file2";
    public static final String INPUT_FILE_NAME_2 = FileHashUtils.createFileNameFromUri(INPUT_FILE_URL_2);

    private static final TeeAppProperties preComputeProperties = TeeAppProperties.builder()
            .image("PRE_COMPUTE_IMAGE")
            .fingerprint(PRE_COMPUTE_FINGERPRINT)
            .entrypoint(PRE_COMPUTE_ENTRYPOINT)
            .heapSizeInBytes(1L)
            .build();

    private static final TeeAppProperties postComputeProperties = TeeAppProperties.builder()
            .image("POST_COMPUTE_IMAGE")
            .fingerprint(POST_COMPUTE_FINGERPRINT)
            .entrypoint(POST_COMPUTE_ENTRYPOINT)
            .heapSizeInBytes(1L)
            .build();

    private static final TeeServicesProperties sconeProperties = new SconeServicesProperties("v5", preComputeProperties, postComputeProperties, "lasImage");

    //region utils
    public static TeeTaskComputeSecret getApplicationDeveloperSecret(String appAddress) {
        return TeeTaskComputeSecret.builder()
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .onChainObjectAddress(appAddress)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .fixedSecretOwner("")
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
        return createSessionRequestBuilder(taskDescription).build();
    }

    public static TeeSessionRequest.TeeSessionRequestBuilder createSessionRequestBuilder(TaskDescription taskDescription) {
        return TeeSessionRequest.builder()
                .sessionId(SESSION_ID)
                .workerAddress(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .taskDescription(taskDescription)
                .teeServicesProperties(sconeProperties);
    }

    public static DealParams.DealParamsBuilder createDealParams() {
        return DealParams.builder()
                .iexecArgs(ARGS)
                .iexecInputFiles(List.of(INPUT_FILE_URL_1, INPUT_FILE_URL_2))
                .iexecResultEncryption(true)
                .iexecResultStorageProvider(STORAGE_PROVIDER)
                .iexecResultStorageProxy(STORAGE_PROXY)
                .iexecSecrets(Map.of("1", REQUESTER_SECRET_KEY_1, "2", REQUESTER_SECRET_KEY_2));
    }

    public static TaskDescription.TaskDescriptionBuilder createTaskDescription(final DealParams dealParams,
                                                                               final TeeEnclaveConfiguration enclaveConfig) {
        final String appAddress = createEthereumAddress();
        final String requesterAddress = createEthereumAddress();
        final String beneficiaryAddress = createEthereumAddress();
        final String workerpoolAddress = createEthereumAddress();
        return TaskDescription.builder()
                .chainDealId(TEST_DEAL_ID)
                .chainTaskId(TASK_ID)
                .workerpoolOwner(workerpoolAddress)
                .appUri(APP_URI)
                .appAddress(appAddress)
                .appEnclaveConfiguration(enclaveConfig)
                .datasetAddress(BytesUtils.EMPTY_ADDRESS)
                .tag(OrderTag.TEE_SCONE.getValue())
                .requester(requesterAddress)
                .beneficiary(beneficiaryAddress)
                .dealParams(dealParams)
                .botSize(1)
                .botFirstIndex(0)
                .botIndex(0);
    }

    public static TaskDescription.TaskDescriptionBuilder createTaskDescriptionWithDataset(final DealParams dealParams,
                                                                                          final TeeEnclaveConfiguration enclaveConfiguration) {
        return createTaskDescription(dealParams, enclaveConfiguration)
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URL)
                .datasetChecksum(DATASET_CHECKSUM);
    }
    //endregion
}
