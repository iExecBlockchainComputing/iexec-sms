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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.iexec.common.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.common.precompute.PreComputeUtils.IEXEC_DATASET_KEY;
import static com.iexec.common.precompute.PreComputeUtils.IS_DATASET_REQUIRED;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;
import static com.iexec.common.tee.TeeUtils.booleanToYesNo;
import static com.iexec.common.worker.result.ResultUtils.*;
import static com.iexec.sms.api.TeeSessionGenerationError.*;

@Slf4j
@Service
public class TeeSecretsService {

    public static final String EMPTY_YML_VALUE = "";

    public static final String INPUT_FILE_URLS = "INPUT_FILE_URLS";
    public static final String INPUT_FILE_NAMES = "INPUT_FILE_NAMES";
    // PreCompute
    public static final String IS_PRE_COMPUTE_REQUIRED = "IS_PRE_COMPUTE_REQUIRED";
    public static final String PRE_COMPUTE_MRENCLAVE = "PRE_COMPUTE_MRENCLAVE";
    // Compute
    public static final String APP_MRENCLAVE = "APP_MRENCLAVE";
    // PostCompute
    public static final String POST_COMPUTE_MRENCLAVE = "POST_COMPUTE_MRENCLAVE";
    // Secrets
    public static final String REQUESTER_SECRETS = "REQUESTER_SECRETS";
    // Env
    private static final String ENV_PROPERTY = "env";

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
                    "Session request must not be null"
            );
        }
        if (request.getTaskDescription() == null) {
            throw new TeeSessionGenerationException(
                    NO_TASK_DESCRIPTION,
                    "Task description must not be null"
            );
        }
        EnclaveEnvironmentsBuilder enclaveEnvironmentsBuilder = EnclaveEnvironments.builder();
        TaskDescription taskDescription = request.getTaskDescription();
        //tokens.put(SESSION_ID, request.getSessionId());
        //enclaveEnvironmentsBuilder.sessionId(request.getSessionId());

        //Map<String, Object> secretsTokens = new HashMap<>();
        //secretsTokens.put(SESSION_ID, request.getSessionId());
        // pre-compute
        boolean isPreComputeRequired = taskDescription.containsDataset() ||
                !taskDescription.getInputFiles().isEmpty();
        //secretsTokens.put(IS_PRE_COMPUTE_REQUIRED, isPreComputeRequired);
        if (isPreComputeRequired) {
            //secretsTokens.putAll(getPreComputeTokens(request));
            enclaveEnvironmentsBuilder.preCompute(getPreComputeTokens(request));
        }
        // app
        //secretsTokens.putAll(getAppTokens(request));
        enclaveEnvironmentsBuilder.appCompute(getAppTokens(request));
        // post compute
        //secretsTokens.putAll(getPostComputeTokens(request));
        enclaveEnvironmentsBuilder.postCompute(getPostComputeTokens(request));
        // env variables
        //TODO: Required?
        //Map<String, String> env = IexecEnvUtils.getAllIexecEnv(taskDescription);
        // Null value should be replaced by an empty string.
        //env.forEach((key, value) -> env.replace(key, null, EMPTY_YML_VALUE));
        //secretsTokens.put(ENV_PROPERTY, env);

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
        EnclaveEnvironmentBuilder environmentBuilder = EnclaveEnvironment.builder();
        environmentBuilder.name("pre-compute");
        Map<String, Object> environmentVariables = new HashMap<>();
        TaskDescription taskDescription = request.getTaskDescription();
        String taskId = taskDescription.getChainTaskId();
        String fingerprint = teeWorkflowConfig.getPreComputeFingerprint();
        environmentBuilder.mrenclave(fingerprint);
        environmentVariables.put(IS_DATASET_REQUIRED, taskDescription.containsDataset());
        environmentVariables.put(IEXEC_DATASET_KEY, EMPTY_YML_VALUE);
        if (taskDescription.containsDataset()) {
            String datasetKey = web3SecretService
                    .getSecret(taskDescription.getDatasetAddress(), true)
                    .orElseThrow(() -> new TeeSessionGenerationException(
                            PRE_COMPUTE_GET_DATASET_SECRET_FAILED,
                            "Empty dataset secret - taskId: " + taskId
                    ))
                    .getTrimmedValue();
            environmentVariables.put(IEXEC_DATASET_KEY, datasetKey);
        } else {
            log.info("No dataset key needed for this task [taskId:{}]", taskId);
        }
        // extract <IEXEC_INPUT_FILE_URL_N, url>
        // this map will be empty (not null) if no input file is found
        Map<String, String> inputFileUrls = IexecEnvUtils.getAllIexecEnv(taskDescription)
                .entrySet()
                .stream()
                .filter(e -> e.getKey().contains(IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        environmentVariables.put(INPUT_FILE_URLS, inputFileUrls);
        environmentBuilder.environment(environmentVariables);
        return environmentBuilder.build();
    }

    /*
     * Compute (App)
     */
    public EnclaveEnvironment getAppTokens(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException{
        EnclaveEnvironmentBuilder environmentBuilder = EnclaveEnvironment.builder();
        environmentBuilder.name("app");
        Map<String, Object> environmentVariables = new HashMap<>();
        TaskDescription taskDescription = request.getTaskDescription();
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(
                    NO_TASK_DESCRIPTION,
                    "Task description must no be null"
            );
        }

        //Map<String, Object> tokens = new HashMap<>();
        TeeEnclaveConfiguration enclaveConfig = taskDescription.getAppEnclaveConfiguration();
        if (enclaveConfig == null) {
            throw new TeeSessionGenerationException(
                    APP_COMPUTE_NO_ENCLAVE_CONFIG,
                    "Enclave configuration must no be null"
            );
        }
        if (!enclaveConfig.getValidator().isValid()){
            throw new TeeSessionGenerationException(
                    APP_COMPUTE_INVALID_ENCLAVE_CONFIG,
                    "Invalid enclave configuration: " +
                            enclaveConfig.getValidator().validate().toString()
            );
        }

        //tokens.put(APP_MRENCLAVE, enclaveConfig.getFingerprint());
        environmentBuilder.mrenclave(enclaveConfig.getFingerprint());
        // extract <IEXEC_INPUT_FILE_NAME_N, name>
        // this map will be empty (not null) if no input file is found
        Map<String, String> inputFileNames = IexecEnvUtils.getComputeStageEnvMap(taskDescription)
                .entrySet()
                .stream()
                .filter(e -> e.getKey().contains(IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if(inputFileNames.size() > 0){
            environmentVariables.put(INPUT_FILE_NAMES, inputFileNames);
        }

        final Map<String, Object> computeSecrets = getApplicationComputeSecrets(taskDescription);
        environmentVariables.putAll(computeSecrets);
       // trusted env variables (not confidential)
        Map<String, String> env = IexecEnvUtils.getAllIexecEnv(taskDescription);
        environmentVariables.putAll(env);
        environmentBuilder.environment(environmentVariables);
        return environmentBuilder.build();
    }

    private Map<String, Object> getApplicationComputeSecrets(TaskDescription taskDescription) {
        final Map<String, Object> tokens = new HashMap<>();
        final String applicationAddress = taskDescription.getAppAddress();

        if (applicationAddress != null) {
            final String secretIndex = "1";
            String appDeveloperSecret =
                    teeTaskComputeSecretService.getSecret(
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
            //tokens.put(REQUESTER_SECRETS, Collections.emptyMap());
            return tokens;
        }

        final HashMap<String, String> requesterSecrets = new HashMap<>();
        for (Map.Entry<String, String> secretEntry: taskDescription.getSecrets().entrySet()) {
            try {
                int requesterSecretIndex = Integer.parseInt(secretEntry.getKey());
                if (requesterSecretIndex <= 0) {
                    String message = "Application secret indices provided in the deal parameters must be positive numbers"
                            + " [providedApplicationSecretIndex:" + requesterSecretIndex + "]";
                    log.warn(message);
                    throw new NumberFormatException(message);
                }
            } catch(NumberFormatException e) {
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
        if(requesterSecrets.size() > 0){
            tokens.put(REQUESTER_SECRETS, requesterSecrets);
        }

        return tokens;
    }

    /*
     * Post-Compute (Result)
     */
    public EnclaveEnvironment getPostComputeTokens(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException {
        EnclaveEnvironmentBuilder environmentBuilder = EnclaveEnvironment.builder();
        environmentBuilder.name("post-compute");
        Map<String, Object> environmentVariables = new HashMap<>();
        TaskDescription taskDescription = request.getTaskDescription();
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(NO_TASK_DESCRIPTION, "Task description must not be null");
        }
        String teePostComputeFingerprint = teeWorkflowConfig.getPostComputeFingerprint();
        // ###############################################################################
        // TODO: activate this when user specific post-compute is properly
        // supported. See https://github.com/iExecBlockchainComputing/iexec-sms/issues/52.
        // ###############################################################################
        // // Use specific post-compute image if requested.
        //if (taskDescription.containsPostCompute()) {
        //    teePostComputeFingerprint = taskDescription.getTeePostComputeFingerprint();
        //    //add entrypoint too
        //}
        environmentVariables.put(POST_COMPUTE_MRENCLAVE, teePostComputeFingerprint);
        // encryption
        Map<String, String> encryptionTokens = getPostComputeEncryptionTokens(request);
        environmentVariables.putAll(encryptionTokens);
        // storage
        Map<String, String> storageTokens = getPostComputeStorageTokens(request);
        environmentVariables.putAll(storageTokens);
        // enclave signature
        Map<String, String> signTokens = getPostComputeSignTokens(request);
        environmentVariables.putAll(signTokens);
        environmentBuilder.environment(environmentVariables);
        return environmentBuilder.build();
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
                    "Empty beneficiary encryption key - taskId: " + taskId
            );
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
        Optional<Secret> requesterStorageTokenSecret =
                web2SecretsService.getSecret(taskDescription.getRequester(), keyName, true);
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
                    "Empty worker address - taskId: " + taskId
            );
        }
        if (StringUtils.isEmpty(request.getEnclaveChallenge())) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_PUBLIC_ENCLAVE_CHALLENGE,
                    "Empty public enclave challenge - taskId: " + taskId
            );
        }
        Optional<TeeChallenge> teeChallenge = teeChallengeService.getOrCreate(taskId, true);
        if (teeChallenge.isEmpty()) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CHALLENGE,
                    "Empty TEE challenge  - taskId: " + taskId
            );
        }
        EthereumCredentials enclaveCredentials = teeChallenge.get().getCredentials();
        if (enclaveCredentials == null || enclaveCredentials.getPrivateKey().isEmpty()) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS,
                    "Empty TEE challenge credentials - taskId: " + taskId
            );
        }
        tokens.put(RESULT_TASK_ID, taskId);
        tokens.put(RESULT_SIGN_WORKER_ADDRESS, workerAddress);
        tokens.put(RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, enclaveCredentials.getPrivateKey());
        return tokens;
    }

}