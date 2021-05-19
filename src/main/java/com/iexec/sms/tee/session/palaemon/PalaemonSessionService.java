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

import com.iexec.common.sms.secret.ReservedSecretKeyName;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.FileHelper;
import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.sms.precompute.PreComputeConfigService;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.StringWriter;
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
import static java.util.Objects.requireNonNull;

@Slf4j
@Service
public class PalaemonSessionService {

    public static final String EMPTY_YML_VALUE = "";

    // Internal values required for setting up a palaemon session
    // Generic
    static final String SESSION_ID_PROPERTY = "SESSION_ID";
    static final String INPUT_FILE_URLS = "INPUT_FILE_URLS";
    static final String INPUT_FILE_NAMES = "INPUT_FILE_NAMES";
    // PreCompute
    static final String IS_PRE_COMPUTE_REQUIRED = "IS_PRE_COMPUTE_REQUIRED";
    static final String PRE_COMPUTE_MRENCLAVE = "PRE_COMPUTE_MRENCLAVE";
    static final String PRE_COMPUTE_FSPF_KEY = "PRE_COMPUTE_FSPF_KEY";
    static final String PRE_COMPUTE_FSPF_TAG = "PRE_COMPUTE_FSPF_TAG";
    // Compute
    static final String APP_FSPF_KEY = "APP_FSPF_KEY";
    static final String APP_FSPF_TAG = "APP_FSPF_TAG";
    static final String APP_MRENCLAVE = "APP_MRENCLAVE";
    static final String APP_ARGS = "APP_ARGS";
    // PostCompute
    static final String POST_COMPUTE_MRENCLAVE = "POST_COMPUTE_MRENCLAVE";
    // Env
    private static final String ENV_PROPERTY = "env";

    private final String palaemonTemplateFilePath;
    private final Web3SecretService web3SecretService;
    private final Web2SecretsService web2SecretsService;
    private final TeeChallengeService teeChallengeService;
    private final PreComputeConfigService preComputeConfigService;


    public PalaemonSessionService(
            @Value("${scone.cas.palaemon}")
            String templateFilePath,
            Web3SecretService web3SecretService,
            Web2SecretsService web2SecretsService,
            TeeChallengeService teeChallengeService,
            PreComputeConfigService preComputeConfigService) throws Exception {
        if (StringUtils.isEmpty(templateFilePath)) {
            throw new IllegalArgumentException("Missing palaemon template filepath");
        }
        if (!FileHelper.exists(templateFilePath)) {
            throw new FileNotFoundException("Missing palaemon template file");
        }
        this.palaemonTemplateFilePath = templateFilePath;
        this.web3SecretService = web3SecretService;
        this.web2SecretsService = web2SecretsService;
        this.teeChallengeService = teeChallengeService;
        this.preComputeConfigService = preComputeConfigService;
    }

    // TODO: Read onchain available infos from enclave instead
    // of copying public vars to palaemon.yml. It needs ssl
    // call from enclave to eth node (only ethereum node address
    // required inside palaemon.yml)
    public String getSessionYml(PalaemonSessionRequest request) throws Exception {
        requireNonNull(request, "Session request must not be null");
        requireNonNull(request.getTaskDescription(), "Task description must not be null");
        TaskDescription taskDescription = request.getTaskDescription();
        Map<String, Object> palaemonTokens = new HashMap<>();
        palaemonTokens.put(SESSION_ID_PROPERTY, request.getSessionId());
        // pre-compute
        boolean isPreComputeRequired = taskDescription.containsDataset() ||
                !taskDescription.getInputFiles().isEmpty();
        palaemonTokens.put(IS_PRE_COMPUTE_REQUIRED, isPreComputeRequired);
        if (isPreComputeRequired) {
            palaemonTokens.putAll(getPreComputePalaemonTokens(request));
        }
        // app
        palaemonTokens.putAll(getAppPalaemonTokens(request));
        // post compute
        palaemonTokens.putAll(getPostComputePalaemonTokens(request));
        // env variables
        Map<String, String> env = IexecEnvUtils.getAllIexecEnv(taskDescription);
        // Null value should be replaced by empty string.
        env.forEach((key, value) -> env.replace(key, null, EMPTY_YML_VALUE));
        palaemonTokens.put(ENV_PROPERTY, env);
        return getFilledPalaemonTemplate(this.palaemonTemplateFilePath, palaemonTokens);
    }

    /**
     * Get tokens to be injected in the pre-compute enclave.
     * 
     * @param request
     * @return map of pre-compute tokens
     * @throws Exception if dataset secret is not found.
     */
    Map<String, Object> getPreComputePalaemonTokens(PalaemonSessionRequest request)
            throws Exception {
        TaskDescription taskDescription = request.getTaskDescription();
        String taskId = taskDescription.getChainTaskId();
        Map<String, Object> tokens = new HashMap<>();
        String fingerprint =
                preComputeConfigService.getConfiguration().getFingerprint();
        tokens.put(PRE_COMPUTE_MRENCLAVE, fingerprint);
        tokens.put(IS_DATASET_REQUIRED, taskDescription.containsDataset());
        tokens.put(IEXEC_DATASET_KEY, EMPTY_YML_VALUE);
        if (taskDescription.containsDataset()) {
            String datasetKey = web3SecretService
                    .getSecret(taskDescription.getDatasetAddress(), true)
                    .orElseThrow(() -> new Exception("Empty dataset secret - taskId: " + taskId))
                    .getTrimmedValue();
            tokens.put(IEXEC_DATASET_KEY, datasetKey);
        } else {
            log.info("No dataset key needed for this task [taskId:{}]", taskId);
        }
        // extract <IEXEC_INPUT_FILE_URL_N, url>
        // this map will be empty (not null) if no input file is found
        Map<String, String> inputFileUrls = IexecEnvUtils.getAllIexecEnv(taskDescription)
                .entrySet()
                .stream()
                .filter(e -> e.getKey().contains(IexecEnvUtils.IEXEC_INPUT_FILE_URL_PREFIX))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        tokens.put(INPUT_FILE_URLS, inputFileUrls);
        return tokens;
    }

    /*
     * Compute (App)
     */
    Map<String, Object> getAppPalaemonTokens(PalaemonSessionRequest request)
            throws Exception {
        TaskDescription taskDescription = request.getTaskDescription();
        requireNonNull(taskDescription, "Task description must no be null");
        Map<String, Object> tokens = new HashMap<>();
        TeeEnclaveConfiguration enclaveConfig = taskDescription.getAppEnclaveConfiguration();
        requireNonNull(enclaveConfig, "Enclave configuration must no be null");
        String fingerprint = enclaveConfig.getFingerprint();
        if (StringUtils.isEmpty(fingerprint)) {
            throw new IllegalArgumentException("Empty app fingerprint: " + fingerprint);
        }
        if (BytesUtils.stringToBytes(fingerprint).length != 32) {
            throw new IllegalArgumentException("Invalid app fingerprint: " + fingerprint);
        }
        tokens.put(APP_MRENCLAVE, fingerprint);
        if (StringUtils.isEmpty(enclaveConfig.getEntrypoint())){
            throw new IllegalArgumentException("Empty app entrypoint: " + fingerprint);
        }
        String appArgs = enclaveConfig.getEntrypoint();
        if (!StringUtils.isEmpty(taskDescription.getCmd())) {
            appArgs = appArgs + " " + taskDescription.getCmd();
        }
        tokens.put(APP_ARGS, appArgs);
        // extract <IEXEC_INPUT_FILE_NAME_N, name>
        // this map will be empty (not null) if no input file is found
        Map<String, String> inputFileNames = IexecEnvUtils.getComputeStageEnvMap(taskDescription)
                .entrySet()
                .stream()
                .filter(e -> e.getKey().contains(IexecEnvUtils.IEXEC_INPUT_FILE_NAME_PREFIX))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        tokens.put(INPUT_FILE_NAMES, inputFileNames);
        return tokens;
    }

    /*
     * Post-Compute (Result)
     */
    Map<String, String> getPostComputePalaemonTokens(PalaemonSessionRequest request)
            throws Exception {
        TaskDescription taskDescription = request.getTaskDescription();
        requireNonNull(taskDescription, "Task description must no be null");
        String taskId = taskDescription.getChainTaskId();
        Map<String, String> tokens = new HashMap<>();
        tokens.put(POST_COMPUTE_MRENCLAVE, taskDescription.getTeePostComputeFingerprint());
        // encryption
        Map<String, String> encryptionTokens = getPostComputeEncryptionTokens(request);
        if (encryptionTokens.isEmpty()) {
            throw new Exception("Failed to get post-compute encryption tokens - taskId: " + taskId);
        }
        tokens.putAll(encryptionTokens);
        // storage
        Map<String, String> storageTokens = getPostComputeStorageTokens(request);
        if (storageTokens.isEmpty()) {
            throw new Exception("Failed to get post-compute storage tokens - taskId: " + taskId);
        }
        tokens.putAll(storageTokens);
        // enclave signature
        Map<String, String> signTokens = getPostComputeSignTokens(request);
        if (signTokens.isEmpty()) {
            throw new Exception("Failed to get post-compute signature tokens - taskId: " + taskId);
        }
        tokens.putAll(signTokens);
        return tokens;
    }

    Map<String, String> getPostComputeEncryptionTokens(PalaemonSessionRequest request)
            throws Exception {
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
            throw new Exception("Empty beneficiary encryption key - taskId: " + taskId);
        }
        String publicKeyValue = beneficiaryResultEncryptionKeySecret.get().getTrimmedValue();
        tokens.put(RESULT_ENCRYPTION_PUBLIC_KEY, publicKeyValue); // base64 encoded by client
        return tokens;
    }

    // TODO: We need a signature of the beneficiary to push
    // to the beneficiary private storage space waiting for
    // that feature we only allow to push to the requester
    // private storage space
    Map<String, String> getPostComputeStorageTokens(PalaemonSessionRequest request)
            throws Exception {
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
            throw new Exception("Empty requester storage token - taskId: " + taskId);
        }
        String requesterStorageToken = requesterStorageTokenSecret.get().getTrimmedValue();
        tokens.put(RESULT_STORAGE_PROVIDER, storageProvider);
        tokens.put(RESULT_STORAGE_PROXY, storageProxy);
        tokens.put(RESULT_STORAGE_TOKEN, requesterStorageToken);
        return tokens;
    }

    Map<String, String> getPostComputeSignTokens(PalaemonSessionRequest request)
            throws Exception {
        String taskId = request.getTaskDescription().getChainTaskId();
        String workerAddress = request.getWorkerAddress();
        Map<String, String> tokens = new HashMap<>();
        if (StringUtils.isEmpty(workerAddress)) {
            throw new Exception("Empty worker address - taskId: " + taskId);
        }
        if (StringUtils.isEmpty(request.getEnclaveChallenge())) {
            throw new Exception("Empty public enclave challenge - taskId: " + taskId);
        }
        Optional<TeeChallenge> teeChallenge = teeChallengeService.getOrCreate(taskId, true);
        if (teeChallenge.isEmpty()) {
            throw new Exception("Empty TEE challenge  - taskId: " + taskId);
        }
        EthereumCredentials enclaveCredentials = teeChallenge.get().getCredentials();
        if (enclaveCredentials == null || enclaveCredentials.getPrivateKey().isEmpty()) {
            throw new Exception("Empty TEE challenge credentials - taskId: " + taskId);
        }
        tokens.put(RESULT_TASK_ID, taskId);
        tokens.put(RESULT_SIGN_WORKER_ADDRESS, workerAddress);
        tokens.put(RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, enclaveCredentials.getPrivateKey());
        return tokens;
    }

    private String getFilledPalaemonTemplate(String templatePath, Map<String, Object> tokens) {
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        Template template = ve.getTemplate(templatePath);
        VelocityContext context = new VelocityContext();
        tokens.forEach(context::put); // copy all data from the tokens into context
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }
}
