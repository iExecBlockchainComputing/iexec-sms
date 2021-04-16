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

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.precompute.PreComputeUtils;
import com.iexec.common.sms.secret.ReservedSecretKeyName;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.precompute.PreComputeConfig;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.fingerprint.AppFingerprint;
import com.iexec.sms.tee.session.fingerprint.PostComputeFingerprint;
import com.iexec.sms.tee.session.fingerprint.PreComputeFingerprint;
import com.iexec.sms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.iexec.common.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.common.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;
import static com.iexec.common.tee.TeeUtils.booleanToYesNo;
import static com.iexec.common.worker.result.ResultUtils.*;

@Slf4j
@Service
public class PalaemonSessionService {

    public static final String EMPTY_YML_VALUE = "''";

    // Internal values required for setting up a palaemon session
    // Generic
    static final String SESSION_ID_PROPERTY = "SESSION_ID";
    static final String IS_DATASET_REQUESTED = "IS_DATASET_REQUESTED";
    // PreCompute
    static final String PRE_COMPUTE_MRENCLAVE = "PRE_COMPUTE_MRENCLAVE";
    static final String PRE_COMPUTE_FSPF_KEY = "PRE_COMPUTE_FSPF_KEY";
    static final String PRE_COMPUTE_FSPF_TAG = "PRE_COMPUTE_FSPF_TAG";
    // Compute
    static final String APP_FSPF_KEY = "APP_FSPF_KEY";
    static final String APP_FSPF_TAG = "APP_FSPF_TAG";
    static final String APP_MRENCLAVE = "APP_MRENCLAVE";
    static final String APP_ARGS = "APP_ARGS";
    // PostCompute
    static final String POST_COMPUTE_FSPF_KEY = "POST_COMPUTE_FSPF_KEY";
    static final String POST_COMPUTE_FSPF_TAG = "POST_COMPUTE_FSPF_TAG";
    static final String POST_COMPUTE_MRENCLAVE = "POST_COMPUTE_MRENCLAVE";
    // Env
    private static final String ENV_PROPERTY = "env";

    @Value("${scone.cas.palaemon}")
    private String palaemonTemplateFilePath;

    private final IexecHubService iexecHubService;
    private final Web3SecretService web3SecretService;
    private final Web2SecretsService web2SecretsService;
    private final TeeChallengeService teeChallengeService;
    private final PreComputeConfig preComputeConfig;

    public PalaemonSessionService(
            IexecHubService iexecHubService,
            Web3SecretService web3SecretService,
            Web2SecretsService web2SecretsService,
            TeeChallengeService teeChallengeService,
            PreComputeConfig preComputeConfig) {
        this.iexecHubService = iexecHubService;
        this.web3SecretService = web3SecretService;
        this.web2SecretsService = web2SecretsService;
        this.teeChallengeService = teeChallengeService;
        this.preComputeConfig = preComputeConfig;
    }

    // TODO: Read onchain available infos from enclave instead
    // of copying public vars to palaemon.yml. It needs ssl
    // call from enclave to eth node (only ethereum node address
    // required inside palaemon.yml)
    public String getSessionYml(PalaemonSessionRequest request) throws Exception {
        String taskId = request.getChainTaskId();
        if (this.palaemonTemplateFilePath.isEmpty()) {
            throw new Exception("Empty Palaemon template filepath - taskId: " + taskId);
        }
        ChainDeal chainDeal = request.getChainDeal();
        Map<String, Object> palaemonTokens = new HashMap<>();
        palaemonTokens.put(SESSION_ID_PROPERTY, request.getSessionId());
        palaemonTokens.put(IS_DATASET_REQUESTED, isDatasetRequested(chainDeal));
        // pre-compute
        palaemonTokens.putAll(getPreComputePalaemonTokens(request));
        // app
        palaemonTokens.putAll(getAppPalaemonTokens(request));
        // post compute
        palaemonTokens.putAll(getPostComputePalaemonTokens(request));
        // env variables
        TaskDescription taskDescription = iexecHubService.getTaskDescription(taskId);
        Map<String, String> env = IexecEnvUtils.getComputeStageEnvMap(taskDescription);
        // All env values should be quoted (even integers) otherwise
        // the CAS fails to parse the session's yaml with the
        // message ("invalid type: integer `0`, expected a string")
        // that's why we add single quotes in palaemonTemplate.vm
        // in the for loop.
        // Null value should be replaced by empty string.
        env.forEach((key, value) -> env.replace(key, null, ""));
        palaemonTokens.put(ENV_PROPERTY, env);
        return getFilledPalaemonTemplate(this.palaemonTemplateFilePath, palaemonTokens);
    }

    /*
     * Pre-Compute
     */
    Map<String, String> getPreComputePalaemonTokens(PalaemonSessionRequest request)
            throws Exception {
        String taskId = request.getChainTaskId();
        ChainDeal chainDeal = request.getChainDeal();
        if (!isDatasetRequested(chainDeal)) {
            log.info("No dataset is requested [taskId:{}]", taskId);
            return Collections.emptyMap();
        }
        Map<String, String> tokens = new HashMap<>();
        String fingerprint = preComputeConfig.getFingerprint();
        PreComputeFingerprint preComputeFingerprint = new PreComputeFingerprint(fingerprint);
        tokens.put(PRE_COMPUTE_MRENCLAVE, preComputeFingerprint.getMrEnclave());
        tokens.put(PRE_COMPUTE_FSPF_KEY, preComputeFingerprint.getFspfKey());
        tokens.put(PRE_COMPUTE_FSPF_TAG, preComputeFingerprint.getFspfTag());
        // set dataset checksum
        if (StringUtils.isBlank(chainDeal.getChainDataset().getChecksum())) {
            throw new Exception("Empty dataset checksum - taskId: " + taskId);
        }
        tokens.put(PreComputeUtils.IEXEC_DATASET_CHECKSUM_PROPERTY,
                chainDeal.getChainDataset().getChecksum());
        // set dataset secret
        String chainDatasetId = chainDeal.getChainDataset().getChainDatasetId();
        Optional<Web3Secret> datasetSecret = web3SecretService.getSecret(chainDatasetId, true);
        if (datasetSecret.isEmpty()) {
            throw new Exception("Empty dataset secret - taskId: " + taskId);
        }
        tokens.put(PreComputeUtils.IEXEC_DATASET_KEY_PROPERTY, datasetSecret.get().getTrimmedValue());
        return tokens;
    }

    /*
     * Compute (App)
     */
    Map<String, String> getAppPalaemonTokens(PalaemonSessionRequest request)
            throws Exception {
        Map<String, String> tokens = new HashMap<>();
        ChainDeal chainDeal = request.getChainDeal();
        if (chainDeal.getChainApp() == null) {
            throw new Exception("Null chain app in deal - taskId: " + request.getChainTaskId());
        }
        AppFingerprint appFingerprint = new AppFingerprint(chainDeal.getChainApp().getFingerprint());
        tokens.put(APP_FSPF_KEY, appFingerprint.getFspfKey());
        tokens.put(APP_FSPF_TAG, appFingerprint.getFspfTag());
        tokens.put(APP_MRENCLAVE, appFingerprint.getMrEnclave());
        String appArgs = appFingerprint.getEntrypoint();
        if (chainDeal.getParams().getIexecArgs() != null && !chainDeal.getParams().getIexecArgs().isEmpty()) {
            appArgs = appFingerprint.getEntrypoint() + " " + chainDeal.getParams().getIexecArgs();
        }
        tokens.put(APP_ARGS, appArgs);
        return tokens;
    }

    /*
     * Post-Compute (Result)
     */
    Map<String, String> getPostComputePalaemonTokens(PalaemonSessionRequest request)
            throws Exception {
        String taskId = request.getChainTaskId();
        ChainDeal chainDeal = request.getChainDeal();
        Map<String, String> tokens = new HashMap<>();
        if (chainDeal.getParams() == null) {
            throw new Exception("Empty deal params - taskId: " + taskId);
        }
        PostComputeFingerprint postComputeFingerprint = new PostComputeFingerprint(
                chainDeal.getParams().getIexecTeePostComputeFingerprint());
        tokens.put(POST_COMPUTE_FSPF_KEY, postComputeFingerprint.getFspfKey());
        tokens.put(POST_COMPUTE_FSPF_TAG, postComputeFingerprint.getFspfTag());
        tokens.put(POST_COMPUTE_MRENCLAVE, postComputeFingerprint.getMrEnclave());
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
        String taskId = request.getChainTaskId();
        ChainDeal chainDeal = request.getChainDeal();
        Map<String, String> tokens = new HashMap<>();
        boolean shouldEncrypt = chainDeal.getParams().isIexecResultEncryption();
        tokens.put(RESULT_ENCRYPTION, booleanToYesNo(shouldEncrypt));
        tokens.put(RESULT_ENCRYPTION_PUBLIC_KEY, EMPTY_YML_VALUE);
        if (!shouldEncrypt) {
            return tokens;
        }
        Optional<Secret> beneficiaryResultEncryptionKeySecret = web2SecretsService.getSecret(
                chainDeal.getBeneficiary(),
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
        String taskId = request.getChainTaskId();
        ChainDeal chainDeal = request.getChainDeal();
        Map<String, String> tokens = new HashMap<>();
        boolean isCallbackRequested = isCallbackRequested(chainDeal);
        tokens.put(RESULT_STORAGE_CALLBACK, booleanToYesNo(isCallbackRequested));
        tokens.put(RESULT_STORAGE_PROVIDER, EMPTY_YML_VALUE);
        tokens.put(RESULT_STORAGE_PROXY, EMPTY_YML_VALUE);
        tokens.put(RESULT_STORAGE_TOKEN, EMPTY_YML_VALUE);
        if (isCallbackRequested) {
            return tokens;
        }
        String storageProvider = chainDeal.getParams().getIexecResultStorageProvider();
        String storageProxy = chainDeal.getParams().getIexecResultStorageProxy();
        String keyName = storageProvider.equals(DROPBOX_RESULT_STORAGE_PROVIDER)
                ? ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN
                : ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN;
        Optional<Secret> requesterStorageTokenSecret = 
                web2SecretsService.getSecret(chainDeal.getRequester(), keyName, true);
        if (requesterStorageTokenSecret.isEmpty()) {
            log.error("Failed to get storage token [taskId:{}, storageProvider:{}, requester:{}]",
                    taskId, storageProvider, chainDeal.getRequester());
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
        String taskId = request.getChainTaskId();
        String workerAddress = request.getWorkerAddress();
        Map<String, String> tokens = new HashMap<>();
        if (workerAddress.isEmpty()) {
            throw new Exception("Empty worker address - taskId: " + taskId);
        }
        if (StringUtils.isBlank(request.getEnclaveChallenge())) {
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

    private boolean isDatasetRequested(ChainDeal chainDeal) {
        return chainDeal.getChainDataset() != null &&
                chainDeal.getChainDataset().getChainDatasetId() != null &&
                !chainDeal.getChainDataset()
                    .getChainDatasetId()
                    .equals(BytesUtils.EMPTY_ADDRESS);
    }

    private boolean isCallbackRequested(ChainDeal chainDeal) {
        return chainDeal.getCallback() != null &&
                !chainDeal.getCallback().equals(BytesUtils.EMPTY_ADDRESS);
    }
}
