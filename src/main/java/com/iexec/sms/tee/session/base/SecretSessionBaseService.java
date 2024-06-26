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
import com.iexec.common.utils.IexecFileHelper;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.secret.compute.*;
import com.iexec.sms.secret.web2.Web2Secret;
import com.iexec.sms.secret.web2.Web2SecretHeader;
import com.iexec.sms.secret.web2.Web2SecretService;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.EthereumCredentials;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.base.SecretEnclaveBase.SecretEnclaveBaseBuilder;
import com.iexec.sms.tee.session.base.SecretSessionBase.SecretSessionBaseBuilder;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.iexec.common.precompute.PreComputeUtils.IS_DATASET_REQUIRED;
import static com.iexec.common.worker.result.ResultUtils.*;
import static com.iexec.commons.poco.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.commons.poco.tee.TeeUtils.booleanToYesNo;
import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static com.iexec.sms.secret.ReservedSecretKeyName.*;

@Slf4j
@Service
public class SecretSessionBaseService {

    static final String EMPTY_YML_VALUE = "";

    public static final String INPUT_FILE_URLS = "INPUT_FILE_URLS";
    public static final String INPUT_FILE_NAMES = "INPUT_FILE_NAMES";
    // PreCompute
    static final String IS_PRE_COMPUTE_REQUIRED = "IS_PRE_COMPUTE_REQUIRED";
    public static final String PRE_COMPUTE_MRENCLAVE = "PRE_COMPUTE_MRENCLAVE";
    static final String IEXEC_PRE_COMPUTE_OUT = "IEXEC_PRE_COMPUTE_OUT";
    static final String IEXEC_DATASET_KEY = "IEXEC_DATASET_KEY";
    // Compute
    public static final String APP_MRENCLAVE = "APP_MRENCLAVE";
    // PostCompute
    public static final String POST_COMPUTE_MRENCLAVE = "POST_COMPUTE_MRENCLAVE";

    private final Web3SecretService web3SecretService;
    private final Web2SecretService web2SecretService;
    private final TeeChallengeService teeChallengeService;
    private final TeeServicesProperties teeServicesConfig;
    private final TeeTaskComputeSecretService teeTaskComputeSecretService;

    public SecretSessionBaseService(
            Web3SecretService web3SecretService,
            Web2SecretService web2SecretService,
            TeeChallengeService teeChallengeService,
            TeeServicesProperties teeServicesConfig,
            TeeTaskComputeSecretService teeTaskComputeSecretService) {
        this.web3SecretService = web3SecretService;
        this.web2SecretService = web2SecretService;
        this.teeChallengeService = teeChallengeService;
        this.teeServicesConfig = teeServicesConfig;
        this.teeTaskComputeSecretService = teeTaskComputeSecretService;
    }

    /**
     * Collect tokens required for different compute stages (pre, in, post).
     *
     * @param request session request details
     * @return All common tokens for a session, whatever TEE technology is used
     */
    public SecretSessionBase getSecretsTokens(TeeSessionRequest request) throws TeeSessionGenerationException {
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
        SecretSessionBaseBuilder sessionBase = SecretSessionBase.builder();
        TaskDescription taskDescription = request.getTaskDescription();
        // pre-compute
        boolean isPreComputeRequired = taskDescription.containsDataset() ||
                !taskDescription.getInputFiles().isEmpty();
        if (isPreComputeRequired) {
            sessionBase.preCompute(getPreComputeTokens(request));
        }
        // app
        sessionBase.appCompute(getAppTokens(request));
        // post compute
        sessionBase.postCompute(getPostComputeTokens(request));
        return sessionBase.build();
    }

    /**
     * Get tokens to be injected in the pre-compute enclave.
     *
     * @param request Session request details
     * @return {@link SecretEnclaveBase} instance
     * @throws TeeSessionGenerationException if dataset secret is not found
     */
    public SecretEnclaveBase getPreComputeTokens(TeeSessionRequest request)
            throws TeeSessionGenerationException {
        SecretEnclaveBaseBuilder enclaveBase = SecretEnclaveBase.builder();
        enclaveBase.name("pre-compute");
        Map<String, Object> tokens = new HashMap<>();
        TaskDescription taskDescription = request.getTaskDescription();
        String taskId = taskDescription.getChainTaskId();
        enclaveBase.mrenclave(teeServicesConfig.getPreComputeProperties().getFingerprint());
        tokens.put(IEXEC_PRE_COMPUTE_OUT, IexecFileHelper.SLASH_IEXEC_IN);
        // `IS_DATASET_REQUIRED` still meaningful?
        tokens.put(IS_DATASET_REQUIRED, taskDescription.containsDataset());

        List<String> trustedEnv = new ArrayList<>();
        if (taskDescription.containsDataset()) {
            String datasetKey = web3SecretService
                    .getDecryptedValue(taskDescription.getDatasetAddress())
                    .orElseThrow(() -> new TeeSessionGenerationException(
                            PRE_COMPUTE_GET_DATASET_SECRET_FAILED,
                            "Empty dataset secret - taskId: " + taskId));
            tokens.put(IEXEC_DATASET_KEY, datasetKey);
            trustedEnv.addAll(List.of(
                    IexecEnvUtils.IEXEC_DATASET_URL,
                    IexecEnvUtils.IEXEC_DATASET_FILENAME,
                    IexecEnvUtils.IEXEC_DATASET_CHECKSUM));
        } else {
            log.info("No dataset key needed for this task [taskId:{}]", taskId);
        }
        trustedEnv.addAll(List.of(
                IexecEnvUtils.IEXEC_TASK_ID,
                IexecEnvUtils.IEXEC_INPUT_FILES_FOLDER,
                IexecEnvUtils.IEXEC_INPUT_FILES_NUMBER));
        IexecEnvUtils.getAllIexecEnv(taskDescription)
                .entrySet()
                .stream()
                .filter(e ->
                        // extract trusted en vars to include
                        trustedEnv.contains(e.getKey())
                                // extract <IEXEC_INPUT_FILE_URL_N, url>
                                || e.getKey().startsWith(IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX))
                .forEach(e -> tokens.put(e.getKey(), e.getValue()));
        return enclaveBase
                .environment(tokens)
                .build();
    }

    /**
     * Get tokens to be injected in the application enclave.
     *
     * @param request Session request details
     * @return {@link SecretEnclaveBase} instance
     * @throws TeeSessionGenerationException if {@code TaskDescription} is {@literal null} or does not contain a {@code TeeEnclaveConfiguration}
     */
    public SecretEnclaveBase getAppTokens(TeeSessionRequest request)
            throws TeeSessionGenerationException {
        SecretEnclaveBaseBuilder enclaveBase = SecretEnclaveBase.builder();
        enclaveBase.name("app");
        TaskDescription taskDescription = request.getTaskDescription();
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(
                    NO_TASK_DESCRIPTION,
                    "Task description must not be null");
        }

        Map<String, Object> tokens = new HashMap<>();
        TeeEnclaveConfiguration enclaveConfig = taskDescription.getAppEnclaveConfiguration();
        if (enclaveConfig == null) {
            throw new TeeSessionGenerationException(
                    APP_COMPUTE_NO_ENCLAVE_CONFIG,
                    "Enclave configuration must not be null");
        }
        if (!enclaveConfig.getValidator().isValid()) {
            throw new TeeSessionGenerationException(
                    APP_COMPUTE_INVALID_ENCLAVE_CONFIG,
                    "Invalid enclave configuration: " +
                            enclaveConfig.getValidator().validate().toString());
        }

        enclaveBase.mrenclave(enclaveConfig.getFingerprint());
        // extract <IEXEC_INPUT_FILE_NAME_N, name>
        // this map will be empty (not null) if no input file is found
        IexecEnvUtils.getComputeStageEnvMap(taskDescription)
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX))
                .forEach(e -> tokens.put(e.getKey(), e.getValue()));

        final Map<String, Object> computeSecrets = getApplicationComputeSecrets(taskDescription);
        tokens.putAll(computeSecrets);
        // trusted env variables (not confidential)
        tokens.putAll(IexecEnvUtils.getComputeStageEnvMap(taskDescription));
        return enclaveBase
                .environment(tokens)
                .build();
    }

    private Map<String, Object> getApplicationComputeSecrets(TaskDescription taskDescription) {
        final Map<String, Object> tokens = new HashMap<>();
        final List<TeeTaskComputeSecretHeader> ids = getAppComputeSecretsHeaders(taskDescription);
        log.debug("TeeTaskComputeSecret looking for secrets [chainTaskId:{}, count:{}]",
                taskDescription.getChainTaskId(), ids.size());
        final List<TeeTaskComputeSecret> secrets = teeTaskComputeSecretService.getSecretsForTeeSession(ids);
        log.debug("TeeTaskComputeSecret objects fetched from database [chainTaskId:{}, count:{}]",
                taskDescription.getChainTaskId(), secrets.size());
        for (TeeTaskComputeSecret secret : secrets) {
            if (!StringUtils.isEmpty(secret.getHeader().getOnChainObjectAddress())) {
                tokens.put("IEXEC_APP_DEVELOPER_SECRET", secret.getValue());
                tokens.put(IexecEnvUtils.IEXEC_APP_DEVELOPER_SECRET_PREFIX + "1", secret.getValue());
            } else {
                final String secretKey = secret.getHeader().getKey();
                taskDescription.getSecrets().forEach((key, value) -> {
                    if (value.equals(secretKey)) {
                        tokens.put(IexecEnvUtils.IEXEC_REQUESTER_SECRET_PREFIX + key, secret.getValue());
                    }
                });
            }
        }
        return tokens;
    }

    private List<TeeTaskComputeSecretHeader> getAppComputeSecretsHeaders(TaskDescription taskDescription) {
        final List<TeeTaskComputeSecretHeader> ids = new ArrayList<>();
        final String applicationAddress = taskDescription.getAppAddress();
        if (applicationAddress != null) {
            final String secretIndex = "1";
            ids.add(new TeeTaskComputeSecretHeader(
                    OnChainObjectType.APPLICATION,
                    applicationAddress.toLowerCase(),
                    SecretOwnerRole.APPLICATION_DEVELOPER,
                    "",
                    secretIndex));
        }

        if (taskDescription.getSecrets() != null && taskDescription.getRequester() != null) {
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
                ids.add(new TeeTaskComputeSecretHeader(
                        OnChainObjectType.APPLICATION,
                        "",
                        SecretOwnerRole.REQUESTER,
                        taskDescription.getRequester().toLowerCase(),
                        secretEntry.getValue()));
            }
        }
        return ids;
    }

    /**
     * Get tokens to be injected in the post-compute enclave.
     *
     * @param request Session request details
     * @return {@link SecretEnclaveBase} instance
     * @throws TeeSessionGenerationException if {@code TaskDescription} is {@literal null}
     */
    public SecretEnclaveBase getPostComputeTokens(TeeSessionRequest request)
            throws TeeSessionGenerationException {
        SecretEnclaveBaseBuilder enclaveBase = SecretEnclaveBase.builder()
                .name("post-compute")
                .mrenclave(teeServicesConfig.getPostComputeProperties().getFingerprint());
        Map<String, Object> tokens = new HashMap<>();
        TaskDescription taskDescription = request.getTaskDescription();
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(NO_TASK_DESCRIPTION, "Task description must not be null");
        }

        final List<Web2SecretHeader> ids = getPostComputeSecretHeaders(taskDescription, request.getWorkerAddress());
        log.debug("Web2Secret looking for secrets [chainTaskId:{}, count:{}]",
                taskDescription.getChainTaskId(), ids.size());
        final List<Web2Secret> secrets = web2SecretService.getSecretsForTeeSession(ids);
        log.debug("Web2Secret objects fetched from database [chainTaskId:{}, count:{}]",
                taskDescription.getChainTaskId(), secrets.size());
        // encryption
        final String resultEncryptionSecret = secrets.stream()
                .filter(secret -> IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY.equals(secret.getHeader().getAddress()))
                .findFirst()
                .map(Web2Secret::getValue)
                .orElse("");
        tokens.putAll(getPostComputeEncryptionTokens(request, resultEncryptionSecret));
        // storage
        if (taskDescription.containsCallback()) {
            tokens.putAll(getPostComputeStorageTokens(request, ""));
        } else if (DROPBOX_RESULT_STORAGE_PROVIDER.equals(taskDescription.getResultStorageProvider())) {
            final String storageToken = secrets.stream()
                    .filter(secret -> IEXEC_RESULT_DROPBOX_TOKEN.equals(secret.getHeader().getAddress()))
                    .findFirst()
                    .map(Web2Secret::getValue)
                    .orElse("");
            tokens.putAll(getPostComputeStorageTokens(request, storageToken));
        } else {
            // TODO remove fallback on requester token when legacy Result Proxy endpoints have been removed
            final boolean isWorkerTokenPresent = secrets.stream()
                    .anyMatch(secret -> IEXEC_RESULT_IEXEC_IPFS_TOKEN.equals(secret.getHeader().getAddress())
                            && request.getWorkerAddress().equalsIgnoreCase(secret.getHeader().getOwnerAddress()));
            final String tokenOwner = isWorkerTokenPresent ? request.getWorkerAddress() : taskDescription.getRequester();
            final String storageToken = secrets.stream()
                    .filter(secret -> IEXEC_RESULT_IEXEC_IPFS_TOKEN.equals(secret.getHeader().getAddress()) &&
                            tokenOwner.equalsIgnoreCase(secret.getHeader().getOwnerAddress()))
                    .findFirst()
                    .map(Web2Secret::getValue)
                    .orElse("");
            log.debug("storage token [isWorkerTokenPresent:{}, tokenOwner:{}]",
                    isWorkerTokenPresent, tokenOwner);
            tokens.putAll(getPostComputeStorageTokens(request, storageToken));
        }
        // enclave signature
        Map<String, String> signTokens = getPostComputeSignTokens(request);
        tokens.putAll(signTokens);
        return enclaveBase
                .environment(tokens)
                .build();
    }

    List<Web2SecretHeader> getPostComputeSecretHeaders(TaskDescription taskDescription, String workerAddress) {
        final List<Web2SecretHeader> ids = new ArrayList<>();
        if (taskDescription.isResultEncryption()) {
            ids.add(new Web2SecretHeader(taskDescription.getBeneficiary(), IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY));
        }
        if (DROPBOX_RESULT_STORAGE_PROVIDER.equals(taskDescription.getResultStorageProvider())) {
            ids.add(new Web2SecretHeader(taskDescription.getRequester(), IEXEC_RESULT_DROPBOX_TOKEN));
        } else {
            ids.add(new Web2SecretHeader(taskDescription.getRequester(), IEXEC_RESULT_IEXEC_IPFS_TOKEN));
            ids.add(new Web2SecretHeader(workerAddress, IEXEC_RESULT_IEXEC_IPFS_TOKEN));
        }
        return ids;
    }

    public Map<String, String> getPostComputeEncryptionTokens(TeeSessionRequest request, String resultEncryptionKey)
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
        if (StringUtils.isEmpty(resultEncryptionKey)) {
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_ENCRYPTION_TOKENS_FAILED_EMPTY_BENEFICIARY_KEY,
                    "Empty beneficiary encryption key - taskId: " + taskId);
        }
        tokens.put(RESULT_ENCRYPTION_PUBLIC_KEY, resultEncryptionKey); // base64 encoded by client
        return tokens;
    }

    // TODO: We need a signature of the beneficiary to push
    // to the beneficiary private storage space waiting for
    // that feature we only allow to push to the requester
    // private storage space
    public Map<String, String> getPostComputeStorageTokens(TeeSessionRequest request, String storageToken)
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
        if (StringUtils.isEmpty(storageToken)) {
            log.error("Failed to get storage token [taskId:{}, storageProvider:{}, requester:{}]",
                    taskId, storageProvider, taskDescription.getRequester());
            throw new TeeSessionGenerationException(
                    POST_COMPUTE_GET_STORAGE_TOKENS_FAILED,
                    "Empty requester storage token - taskId: " + taskId);
        }
        tokens.put(RESULT_STORAGE_PROVIDER, storageProvider);
        tokens.put(RESULT_STORAGE_PROXY, storageProxy);
        tokens.put(RESULT_STORAGE_TOKEN, storageToken);
        return tokens;
    }

    public Map<String, String> getPostComputeSignTokens(TeeSessionRequest request)
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
