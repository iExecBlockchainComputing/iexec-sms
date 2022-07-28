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

import com.iexec.common.sms.secret.ReservedSecretKeyName;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.common.utils.IexecFileHelper;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.secret.compute.OnChainObjectType;
import com.iexec.sms.secret.compute.SecretOwnerRole;
import com.iexec.sms.secret.compute.TeeTaskComputeSecret;
import com.iexec.sms.secret.compute.TeeTaskComputeSecretService;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.EnclaveEnvironment.EnclaveEnvironmentBuilder;
import com.iexec.sms.tee.session.EnclaveEnvironments.EnclaveEnvironmentsBuilder;
import com.iexec.sms.tee.session.generic.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import com.iexec.sms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.iexec.common.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.common.precompute.PreComputeUtils.IS_DATASET_REQUIRED;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;
import static com.iexec.common.tee.TeeUtils.booleanToYesNo;
import static com.iexec.common.worker.result.ResultUtils.*;
import static com.iexec.sms.api.TeeSessionGenerationError.*;

@Slf4j
@Service
public class TeeSecretsService {

    static final String EMPTY_YML_VALUE = "";

    static final String INPUT_FILE_URLS = "INPUT_FILE_URLS";
    static final String INPUT_FILE_NAMES = "INPUT_FILE_NAMES";
    // PreCompute
    static final String IS_PRE_COMPUTE_REQUIRED = "IS_PRE_COMPUTE_REQUIRED";
    static final String PRE_COMPUTE_MRENCLAVE = "PRE_COMPUTE_MRENCLAVE";
    static final String IEXEC_PRE_COMPUTE_OUT = "IEXEC_PRE_COMPUTE_OUT";
    static final String IEXEC_DATASET_KEY = "IEXEC_DATASET_KEY";
    // Compute
    static final String APP_MRENCLAVE = "APP_MRENCLAVE";
    // PostCompute
    static final String POST_COMPUTE_MRENCLAVE = "POST_COMPUTE_MRENCLAVE";

    private final Web3SecretService web3SecretService;
    private final Web2SecretsService web2SecretsService;
    private final TeeChallengeService teeChallengeService;
    private final TeeWorkflowConfiguration teeWorkflowConfig;
    private final TeeTaskComputeSecretService teeTaskComputeSecretService;

    public TeeSecretsService(
            Web3SecretService web3SecretService,
            Web2SecretsService web2SecretsService,
            TeeChallengeService teeChallengeService,
            TeeWorkflowConfiguration teeWorkflowConfig,
            TeeTaskComputeSecretService teeTaskComputeSecretService) {
        this.web3SecretService = web3SecretService;
        this.web2SecretsService = web2SecretsService;
        this.teeChallengeService = teeChallengeService;
        this.teeWorkflowConfig = teeWorkflowConfig;
        this.teeTaskComputeSecretService = teeTaskComputeSecretService;
    }

    /**
     * Collect tokens required for different compute stages (pre, in, post).
     *
     * @param request session request details
     * @return All common tokens for a session, whatever TEE technology is used
     */
    public EnclaveEnvironments getSecretsTokens(TeeSecretsSessionRequest request) throws TeeSessionGenerationException {
        if (request == null) {
            throw new TeeSessionGenerationException(
                    NO_SESSION_REQUEST,
                    "Session request must not be null");
        }
        if (request.getTaskDescription() == null) {
            throw new TeeSessionGenerationException(
                    NO_TASK_DESCRIPTION,
                    "Task description must not be null");
        }
        EnclaveEnvironmentsBuilder enclaveEnvironmentsBuilder = EnclaveEnvironments.builder();
        TaskDescription taskDescription = request.getTaskDescription();
        // pre-compute
        boolean isPreComputeRequired = taskDescription.containsDataset() ||
                !taskDescription.getInputFiles().isEmpty();
        if (isPreComputeRequired) {
            enclaveEnvironmentsBuilder.preCompute(getPreComputeTokens(request));
        }
        // app
        enclaveEnvironmentsBuilder.appCompute(getAppTokens(request));
        // post compute
        enclaveEnvironmentsBuilder.postCompute(getPostComputeTokens(request));
        return enclaveEnvironmentsBuilder.build();
    }

    /**
     * Get tokens to be injected in the pre-compute enclave.
     *
     * @return map of pre-compute tokens
     * @throws TeeSessionGenerationException if dataset secret is not found.
     */
    public EnclaveEnvironment getPreComputeTokens(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException {
        EnclaveEnvironmentBuilder builder = EnclaveEnvironment.builder();
        builder.name("pre-compute");
        Map<String, Object> tokens = new HashMap<>();
        TaskDescription taskDescription = request.getTaskDescription();
        String taskId = taskDescription.getChainTaskId();
        builder.mrenclave(teeWorkflowConfig.getPreComputeFingerprint());
        tokens.put(IEXEC_PRE_COMPUTE_OUT, IexecFileHelper.SLASH_IEXEC_IN);
        // `IS_DATASET_REQUIRED` still meaningful?
        tokens.put(IS_DATASET_REQUIRED, taskDescription.containsDataset());
        if (taskDescription.containsDataset()) {
            String datasetKey = web3SecretService
                    .getSecret(taskDescription.getDatasetAddress(), true)
                    .orElseThrow(() -> new TeeSessionGenerationException(
                            PRE_COMPUTE_GET_DATASET_SECRET_FAILED,
                            "Empty dataset secret - taskId: " + taskId))
                    .getTrimmedValue();
            tokens.put(IEXEC_DATASET_KEY, datasetKey);
        } else {
            log.info("No dataset key needed for this task [taskId:{}]", taskId);
        }
        List<String> trustedKeys = List.of(
                IexecEnvUtils.IEXEC_TASK_ID,
                IexecEnvUtils.IEXEC_DATASET_URL,
                IexecEnvUtils.IEXEC_DATASET_FILENAME,
                IexecEnvUtils.IEXEC_DATASET_CHECKSUM,
                IexecEnvUtils.IEXEC_INPUT_FILES_FOLDER,
                IexecEnvUtils.IEXEC_INPUT_FILES_NUMBER);
        Map<String, String> trustedEnvVars = IexecEnvUtils.getAllIexecEnv(taskDescription)
                .entrySet()
                .stream()
                .filter(e ->
                // extract trusted en vars to include
                trustedKeys.contains(e.getKey())
                        // extract <IEXEC_INPUT_FILE_URL_N, url>
                        || e.getKey().contains(IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        tokens.putAll(trustedEnvVars);
        return builder
                .environment(tokens)
                .build();
    }

    /*
     * Compute (App)
     */
    public EnclaveEnvironment getAppTokens(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException {
        EnclaveEnvironmentBuilder builder = EnclaveEnvironment.builder();
        builder.name("app");
        TaskDescription taskDescription = request.getTaskDescription();
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(
                    NO_TASK_DESCRIPTION,
                    "Task description must no be null");
        }

        Map<String, Object> tokens = new HashMap<>();
        TeeEnclaveConfiguration enclaveConfig = taskDescription.getAppEnclaveConfiguration();
        if (enclaveConfig == null) {
            throw new TeeSessionGenerationException(
                    APP_COMPUTE_NO_ENCLAVE_CONFIG,
                    "Enclave configuration must no be null");
        }
        if (!enclaveConfig.getValidator().isValid()) {
            throw new TeeSessionGenerationException(
                    APP_COMPUTE_INVALID_ENCLAVE_CONFIG,
                    "Invalid enclave configuration: " +
                            enclaveConfig.getValidator().validate().toString());
        }

        builder.mrenclave(enclaveConfig.getFingerprint());
        // extract <IEXEC_INPUT_FILE_NAME_N, name>
        // this map will be empty (not null) if no input file is found
        Map<String, String> inputFileNames = IexecEnvUtils.getComputeStageEnvMap(taskDescription)
                .entrySet()
                .stream()
                .filter(e -> e.getKey().contains(IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        tokens.putAll(inputFileNames);

        final Map<String, Object> computeSecrets = getApplicationComputeSecrets(taskDescription);
        tokens.putAll(computeSecrets);
        // trusted env variables (not confidential)
        tokens.putAll(IexecEnvUtils.getComputeStageEnvMap(taskDescription));
        return builder
                .environment(tokens)
                .build();
    }

    private Map<String, Object> getApplicationComputeSecrets(TaskDescription taskDescription) {
        final Map<String, Object> tokens = new HashMap<>();
        final String applicationAddress = taskDescription.getAppAddress();

        if (applicationAddress != null) {
            final String secretIndex = "1";
            String appDeveloperSecret = teeTaskComputeSecretService.getSecret(
                    OnChainObjectType.APPLICATION,
                    applicationAddress.toLowerCase(),
                    SecretOwnerRole.APPLICATION_DEVELOPER,
                    "",
                    secretIndex)
                    .map(TeeTaskComputeSecret::getValue)
                    .orElse(EMPTY_YML_VALUE);
            tokens.put(IexecEnvUtils.IEXEC_APP_DEVELOPER_SECRET_PREFIX + secretIndex, appDeveloperSecret);
        }

        if (taskDescription.getSecrets() == null || taskDescription.getRequester() == null) {
            return tokens;
        }

        final HashMap<String, String> requesterSecrets = new HashMap<>();
        for (Map.Entry<String, String> secretEntry : taskDescription.getSecrets().entrySet()) {
            try {
                int requesterSecretIndex = Integer.parseInt(secretEntry.getKey());
                if (requesterSecretIndex <= 0) {
                    String message = "Application secret indices provided in the deal parameters must be positive numbers"
                            + " [providedApplicationSecretIndex:" + requesterSecretIndex + "]";
                    log.warn(message);
                    throw new NumberFormatException(message);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid entry found in deal parameters secrets map", e);
                continue;
            }
            String requesterSecret = teeTaskComputeSecretService.getSecret(
                    OnChainObjectType.APPLICATION,
                    "",
                    SecretOwnerRole.REQUESTER,
                    taskDescription.getRequester().toLowerCase(),
                    secretEntry.getValue())
                    .map(TeeTaskComputeSecret::getValue)
                    .orElse(EMPTY_YML_VALUE);
            requesterSecrets.put(IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + secretEntry.getKey(), requesterSecret);
        }
        tokens.putAll(requesterSecrets);
        return tokens;
    }

    /*
     * Post-Compute (Result)
     */
    public EnclaveEnvironment getPostComputeTokens(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException {
        EnclaveEnvironmentBuilder builder = EnclaveEnvironment.builder();
        builder.name("post-compute");
        Map<String, Object> tokens = new HashMap<>();
        TaskDescription taskDescription = request.getTaskDescription();
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(NO_TASK_DESCRIPTION, "Task description must not be null");
        }
        String teePostComputeFingerprint = teeWorkflowConfig.getPostComputeFingerprint();
        // ###############################################################################
        // TODO: activate this when user specific post-compute is properly
        // supported. See
        // https://github.com/iExecBlockchainComputing/iexec-sms/issues/52.
        // ###############################################################################
        // // Use specific post-compute image if requested.
        // if (taskDescription.containsPostCompute()) {
        // teePostComputeFingerprint = taskDescription.getTeePostComputeFingerprint();
        // //add entrypoint too
        // }
        tokens.put(POST_COMPUTE_MRENCLAVE, teePostComputeFingerprint);
        // encryption
        Map<String, String> encryptionTokens = getPostComputeEncryptionTokens(request);
        tokens.putAll(encryptionTokens);
        // storage
        Map<String, String> storageTokens = getPostComputeStorageTokens(request);
        tokens.putAll(storageTokens);
        // enclave signature
        Map<String, String> signTokens = getPostComputeSignTokens(request);
        tokens.putAll(signTokens);
        return builder
                .environment(tokens)
                .build();
    }

    public Map<String, String> getPostComputeEncryptionTokens(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException {
        TaskDescription taskDescription = request.getTaskDescription();
        String taskId = taskDescription.getChainTaskId();
        Map<String, String> tokens = new HashMap<>();
        boolean shouldEncrypt = taskDescription.isResultEncryption();
        // TODO use boolean with quotes instead of yes/no
        tokens.put(RESULT_ENCRYPTION, booleanToYesNo(shouldEncrypt));
        tokens.put(RESULT_ENCRYPTION_PUBLIC_KEY, EMPTY_YML_VALUE);
        if (!shouldEncrypt) {
            return tokens;
        }
        Optional<Secret> beneficiaryResultEncryptionKeySecret = web2SecretsService.getSecret(
                taskDescription.getBeneficiary(),
                IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY,
                true);
        if (beneficiaryResultEncryptionKeySecret.isEmpty()) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_ENCRYPTION_TOKENS_FAILED_EMPTY_BENEFICIARY_KEY,
                    "Empty beneficiary encryption key - taskId: " + taskId);
        }
        String publicKeyValue = beneficiaryResultEncryptionKeySecret.get().getTrimmedValue();
        tokens.put(RESULT_ENCRYPTION_PUBLIC_KEY, publicKeyValue); // base64 encoded by client
        return tokens;
    }

    // TODO: We need a signature of the beneficiary to push
    // to the beneficiary private storage space waiting for
    // that feature we only allow to push to the requester
    // private storage space
    public Map<String, String> getPostComputeStorageTokens(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException {
        TaskDescription taskDescription = request.getTaskDescription();
        String taskId = taskDescription.getChainTaskId();
        Map<String, String> tokens = new HashMap<>();
        boolean isCallbackRequested = taskDescription.containsCallback();
        tokens.put(RESULT_STORAGE_CALLBACK, booleanToYesNo(isCallbackRequested));
        tokens.put(RESULT_STORAGE_PROVIDER, EMPTY_YML_VALUE);
        tokens.put(RESULT_STORAGE_PROXY, EMPTY_YML_VALUE);
        tokens.put(RESULT_STORAGE_TOKEN, EMPTY_YML_VALUE);
        if (isCallbackRequested) {
            return tokens;
        }
        String storageProvider = taskDescription.getResultStorageProvider();
        String storageProxy = taskDescription.getResultStorageProxy();
        String keyName = storageProvider.equals(DROPBOX_RESULT_STORAGE_PROVIDER)
                ? ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN
                : ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN;
        Optional<Secret> requesterStorageTokenSecret = web2SecretsService.getSecret(taskDescription.getRequester(),
                keyName, true);
        if (requesterStorageTokenSecret.isEmpty()) {
            log.error("Failed to get storage token [taskId:{}, storageProvider:{}, requester:{}]",
                    taskId, storageProvider, taskDescription.getRequester());
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_STORAGE_TOKENS_FAILED,
                    "Empty requester storage token - taskId: " + taskId);
        }
        String requesterStorageToken = requesterStorageTokenSecret.get().getTrimmedValue();
        tokens.put(RESULT_STORAGE_PROVIDER, storageProvider);
        tokens.put(RESULT_STORAGE_PROXY, storageProxy);
        tokens.put(RESULT_STORAGE_TOKEN, requesterStorageToken);
        return tokens;
    }

    public Map<String, String> getPostComputeSignTokens(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException {
        String taskId = request.getTaskDescription().getChainTaskId();
        String workerAddress = request.getWorkerAddress();
        Map<String, String> tokens = new HashMap<>();
        if (StringUtils.isEmpty(workerAddress)) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_WORKER_ADDRESS,
                    "Empty worker address - taskId: " + taskId);
        }
        if (StringUtils.isEmpty(request.getEnclaveChallenge())) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_PUBLIC_ENCLAVE_CHALLENGE,
                    "Empty public enclave challenge - taskId: " + taskId);
        }
        Optional<TeeChallenge> teeChallenge = teeChallengeService.getOrCreate(taskId, true);
        if (teeChallenge.isEmpty()) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CHALLENGE,
                    "Empty TEE challenge  - taskId: " + taskId);
        }
        EthereumCredentials enclaveCredentials = teeChallenge.get().getCredentials();
        if (enclaveCredentials == null || enclaveCredentials.getPrivateKey().isEmpty()) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS,
                    "Empty TEE challenge credentials - taskId: " + taskId);
        }
        tokens.put(RESULT_TASK_ID, taskId);
        tokens.put(RESULT_SIGN_WORKER_ADDRESS, workerAddress);
        tokens.put(RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, enclaveCredentials.getPrivateKey());
        return tokens;
    }

}
