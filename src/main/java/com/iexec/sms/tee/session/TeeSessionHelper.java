package com.iexec.sms.tee.session;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.EnvUtils;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.common.sms.secret.ReservedSecretKeyName;
import com.iexec.common.task.TaskDescription;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.fingerprint.AppFingerprint;
import com.iexec.sms.tee.session.fingerprint.DatasetFingerprint;
import com.iexec.sms.tee.session.fingerprint.FingerprintUtils;
import com.iexec.sms.tee.session.fingerprint.PostComputeFingerprint;
import com.iexec.sms.utils.EthereumCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.iexec.common.chain.DealParams.DROPBOX_RESULT_STORAGE_PROVIDER;
import static com.iexec.common.chain.DealParams.IPFS_RESULT_STORAGE_PROVIDER;
import static com.iexec.common.tee.TeeUtils.booleanToYesNo;
import static com.iexec.common.worker.result.ResultUtils.*;

@Service
@Slf4j
public class TeeSessionHelper {

    public static final String EMPTY_YML_VALUE = "''";

    /*
     * Internal values required for setting up a palaemon session
     * */
    private static final String SESSION_ID_PROPERTY = "SESSION_ID";
    private static final String APP_FSPF_KEY = "APP_FSPF_KEY";
    private static final String APP_FSPF_TAG = "APP_FSPF_TAG";
    private static final String APP_MRENCLAVE = "APP_MRENCLAVE";
    private static final String APP_ARGS = "APP_ARGS";
    private static final String DATASET_FSPF_KEY = "DATA_FSPF_KEY";
    private static final String DATASET_FSPF_TAG = "DATA_FSPF_TAG";
    private static final String POST_COMPUTE_FSPF_KEY = "POST_COMPUTE_FSPF_KEY";
    private static final String POST_COMPUTE_FSPF_TAG = "POST_COMPUTE_FSPF_TAG";
    private static final String POST_COMPUTE_MRENCLAVE = "POST_COMPUTE_MRENCLAVE";
    private static final String IS_DATASET_REQUESTED = "IS_DATASET_REQUESTED";

    private TeeSessionHelperConfiguration teeSessionHelperConfiguration;
    private IexecHubService iexecHubService;
    private Web3SecretService web3SecretService;
    private Web2SecretsService web2SecretsService;
    private TeeChallengeService teeChallengeService;

    public TeeSessionHelper(
            TeeSessionHelperConfiguration teeSessionHelperConfiguration,
            IexecHubService iexecHubService,
            Web3SecretService web3SecretService,
            Web2SecretsService web2SecretsService,
            TeeChallengeService teeChallengeService) {
        this.teeSessionHelperConfiguration = teeSessionHelperConfiguration;
        this.iexecHubService = iexecHubService;
        this.web3SecretService = web3SecretService;
        this.web2SecretsService = web2SecretsService;
        this.teeChallengeService = teeChallengeService;
    }

    public String getPalaemonSessionYmlAsString(String sessionId, String taskId, String workerAddress, String attestingEnclave) {
        Optional<ChainTask> oChainTask = iexecHubService.getChainTask(taskId);
        if (!oChainTask.isPresent()) {
            log.error("Failed to getPalaemonSessionYmlAsString (getChainTask failed)[taskId:{}]", taskId);
            return "";
        }
        ChainTask chainTask = oChainTask.get();
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainTask.getDealid());
        if (!oChainDeal.isPresent()) {
            log.error("Failed to getPalaemonSessionYmlAsString (getChainDeal failed)[taskId:{}]", taskId);
            return "";
        }
        ChainDeal chainDeal = oChainDeal.get();

        String palaemonTemplatePath = teeSessionHelperConfiguration.getPalaemonTemplate();
        if (palaemonTemplatePath.isEmpty()) {
            log.error("Failed to getPalaemonSessionYmlAsString (empty templatePath)[taskId:{}]", taskId);
            return "";
        }

        Map<String, Object> palaemonTokens = getPalaemonTokens(sessionId, taskId, workerAddress, attestingEnclave, chainDeal);
        if (palaemonTokens.isEmpty()) {
            log.error("Failed to getPalaemonSessionYmlAsString (empty palaemonTokens)[taskId:{}]", taskId);
            return "";
        }

        return getDocumentFilledWithTokens(palaemonTemplatePath, palaemonTokens);
    }

    // TODO Read onchain available infos from enclave instead of copying public vars to palaemon.yml
    //  It needs ssl call from enclave to eth node (only ethereum node address required inside palaemon.yml)
    private Map<String, Object> getPalaemonTokens(String sessionId, String taskId, String workerAddress, String attestingEnclave, ChainDeal chainDeal) {
        Map<String, Object> palaemonTokens = new HashMap<>();
        palaemonTokens.put(SESSION_ID_PROPERTY, sessionId);
        // app
        Map<String, String> appTokens = getAppPalaemonTokens(taskId, chainDeal);
        if (appTokens.isEmpty()) {
            log.error("Failed to getPalaemonTokens (empty appTokens)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }
        palaemonTokens.putAll(appTokens);
        // post compute
        Map<String, String> postComputeTokens = getPostComputePalaemonTokens(taskId, chainDeal, workerAddress, attestingEnclave);
        if (postComputeTokens.isEmpty()) {
            log.error("Failed to getPalaemonTokens (empty postComputeTokens)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }
        palaemonTokens.putAll(postComputeTokens);
        // dataset
        boolean isDatasetRequested = isDatasetRequested(chainDeal);
        palaemonTokens.put(IS_DATASET_REQUESTED, isDatasetRequested);
        if (isDatasetRequested) {
            Map<String, String> datasetTokens = getDatasetPalaemonTokens(taskId, chainDeal);
            if (datasetTokens.isEmpty()) {
                log.error("Failed to getPalaemonTokens (empty datasetTokens)[taskId:{}]", taskId);
                return Collections.emptyMap();
            }
            palaemonTokens.putAll(datasetTokens);
        }
        // env variables
        TaskDescription taskDescription = iexecHubService.getTaskDescription(taskId);
        Map<String, String> env = EnvUtils.getContainerEnvMap(taskDescription);
        /* 
         * All values should be quoted (even integers) otherwise
         * the CAS fails to parse the session's yaml with the
         * message ("invalid type: integer `0`, expected a string")
         * that's why we add single quotes in palaemonTemplate.vm
         * in the for loop.
         * Null value should be replaced by empty string.
         */
        env.forEach((key, value) -> env.replace(key, null, ""));
        palaemonTokens.put("env", env);
        return palaemonTokens;
    }

    /*
     * Compute (App)
     * */
    private Map<String, String> getAppPalaemonTokens(String taskId, ChainDeal chainDeal) {
        Map<String, String> tokens = new HashMap<>();

        AppFingerprint appFingerprint = FingerprintUtils.toAppFingerprint(chainDeal.getChainApp().getFingerprint());

        if (appFingerprint == null) {
            log.error("Failed to getAppPalaemonTokens (null appFingerprint)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }
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
     * Pre-Compute (Optional Dataset)
     * */
    private Map<String, String> getDatasetPalaemonTokens(String taskId, ChainDeal chainDeal) {
        Map<String, String> tokens = new HashMap<>();

        if (!isDatasetRequested(chainDeal)) {
            log.error("Failed to getDatasetPalaemonTokens (no dataset requested)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }

        String chainDatasetId = chainDeal.getChainDataset().getChainDatasetId();
        Optional<Web3Secret> datasetSecret = web3SecretService.getSecret(chainDatasetId, true);

        if (datasetSecret.isEmpty()) {
            log.error("Failed to getDatasetPalaemonTokens (empty datasetSecret)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }

        DatasetFingerprint datasetFingerprint = FingerprintUtils
                .toDatasetFingerprint(datasetSecret.get().getValue());

        if (datasetFingerprint == null) {
            log.error("Failed to getDatasetPalaemonTokens (null datasetFingerprint)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }
        tokens.put(DATASET_FSPF_KEY, datasetFingerprint.getFspfKey());
        tokens.put(DATASET_FSPF_TAG, datasetFingerprint.getFspfTag());

        return tokens;
    }

    /*
     * Post-Compute (Result)
     * */
    private Map<String, String> getPostComputePalaemonTokens(String taskId, ChainDeal chainDeal, String workerAddress, String attestingEnclave) {
        Map<String, String> tokens = new HashMap<>();

        PostComputeFingerprint postComputeFingerprint = FingerprintUtils
                .toPostComputeFingerprint(chainDeal.getParams().getIexecTeePostComputeFingerprint());

        if (postComputeFingerprint == null) {
            log.error("Failed to getAppPalaemonTokens (null postComputeFingerprint)");
            return Collections.emptyMap();
        }
        tokens.put(POST_COMPUTE_FSPF_KEY, postComputeFingerprint.getFspfKey());
        tokens.put(POST_COMPUTE_FSPF_TAG, postComputeFingerprint.getFspfTag());
        tokens.put(POST_COMPUTE_MRENCLAVE, postComputeFingerprint.getMrEnclave());

        Map<String, String> encryptionTokens = getPostComputeEncryptionTokens(taskId, chainDeal);
        if (encryptionTokens.isEmpty()) {
            log.error("Failed to getPostComputePalaemonTokens (empty encryptionTokens)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }
        tokens.putAll(encryptionTokens);

        Map<String, String> storageTokens = getPostComputeStorageTokens(taskId, chainDeal);
        if (storageTokens.isEmpty()) {
            log.error("Failed to getPostComputePalaemonTokens (empty storageTokens)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }
        tokens.putAll(storageTokens);

        Map<String, String> signTokens = getPostComputeSignTokens(taskId, workerAddress, attestingEnclave);
        if (signTokens.isEmpty()) {
            log.error("Failed to getPostComputePalaemonTokens (empty signTokens)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }
        tokens.putAll(signTokens);

        return tokens;
    }

    // TODO: We need a signature of the beneficiary to push to the beneficiary private storage space
    //  waiting for that feature we only allow to push to the requester private storage space
    private Map<String, String> getPostComputeStorageTokens(String taskId, ChainDeal chainDeal) {
        Map<String, String> tokens = new HashMap<>();

        boolean isStorageCallback = isCallbackRequested(chainDeal);
        String storageProvider = EMPTY_YML_VALUE;//TODO Move empty to templating
        String requesterStorageToken = EMPTY_YML_VALUE;
        String storageProxy = EMPTY_YML_VALUE;

        if (!isStorageCallback) {
            storageProvider = chainDeal.getParams().getIexecResultStorageProvider();
            storageProxy = chainDeal.getParams().getIexecResultStorageProxy();

            Optional<Secret> requesterStorageTokenSecret;

            switch (storageProvider) {
                case DROPBOX_RESULT_STORAGE_PROVIDER:
                    requesterStorageTokenSecret = web2SecretsService.getSecret(chainDeal.getRequester(),
                            ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN, true);
                    break;
                case IPFS_RESULT_STORAGE_PROVIDER:
                default:
                    requesterStorageTokenSecret = web2SecretsService.getSecret(chainDeal.getRequester(),
                            ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN, true);
                    break;
            }

            if (requesterStorageTokenSecret.isEmpty()) {
                log.error("Failed to getPostComputeStorageTokens (empty requesterStorageTokenSecret)[taskId:{}]", taskId);
                return Collections.emptyMap();
            }

            requesterStorageToken = requesterStorageTokenSecret.get().getValue();

        }

        tokens.put(RESULT_STORAGE_CALLBACK, booleanToYesNo(isStorageCallback));
        tokens.put(RESULT_STORAGE_PROVIDER, storageProvider);
        tokens.put(RESULT_STORAGE_PROXY, storageProxy);
        tokens.put(RESULT_STORAGE_TOKEN, requesterStorageToken);

        return tokens;
    }


    private Map<String, String> getPostComputeEncryptionTokens(String taskId, ChainDeal chainDeal) {
        Map<String, String> tokens = new HashMap<>();

        boolean shouldEncrypt = chainDeal.getParams().isIexecResultEncryption();
        String beneficiaryResultEncryptionKey = EMPTY_YML_VALUE;

        if (shouldEncrypt) {
            Optional<Secret> beneficiaryResultEncryptionKeySecret = web2SecretsService.getSecret(chainDeal.getBeneficiary(),
                    ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY, true);
            if (beneficiaryResultEncryptionKeySecret.isEmpty()) {
                log.error("Failed to getPostComputeEncryptionTokens (empty beneficiaryResultEncryptionKeySecret)[taskId:{}]", taskId);
                return Collections.emptyMap();
            }
            beneficiaryResultEncryptionKey = beneficiaryResultEncryptionKeySecret.get().getValue();
        }

        tokens.put(RESULT_ENCRYPTION, booleanToYesNo(shouldEncrypt));
        tokens.put(RESULT_ENCRYPTION_PUBLIC_KEY, beneficiaryResultEncryptionKey);//base64 encoded by client

        return tokens;
    }

    private Map<String, String> getPostComputeSignTokens(String taskId, String workerAddress, String attestingEnclave) {
        Map<String, String> tokens = new HashMap<>();
        if (workerAddress.isEmpty()) {
            log.error("Failed to getPostComputeSignTokens (empty workerAddress)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }

        //TODO See if we could avoid passing attestingEnclave since already in task descryption
        Optional<TeeChallenge> executionAttestor = teeChallengeService.getOrCreate(taskId, true);
        if (attestingEnclave.isEmpty() || executionAttestor.isEmpty()) {
            log.error("Failed to getPostComputeSignTokens (empty attestingEnclave or executionAttestor)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }
        EthereumCredentials enclaveCredentials = executionAttestor.get().getCredentials();
        if (enclaveCredentials == null || enclaveCredentials.getPrivateKey().isEmpty()) {
            log.error("Failed to getPostComputeSignTokens (empty enclaveCredentials)[taskId:{}]", taskId);
            return Collections.emptyMap();
        }

        tokens.put(RESULT_TASK_ID, taskId);
        tokens.put(RESULT_SIGN_WORKER_ADDRESS, workerAddress);
        tokens.put(RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, enclaveCredentials.getPrivateKey());

        return tokens;
    }

    private String getDocumentFilledWithTokens(String templatePath, Map<String, Object> tokens) {
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        Template template = ve.getTemplate(templatePath);
        VelocityContext context = new VelocityContext();
        tokens.forEach(context::put);// copy all data from the tokens into context
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    private boolean isDatasetRequested(ChainDeal chainDeal) {
        return chainDeal.getChainDataset() != null && !chainDeal.getChainDataset().getChainDatasetId().equals(BytesUtils.EMPTY_ADDRESS);
    }

    private boolean isCallbackRequested(ChainDeal chainDeal) {
        return chainDeal.getCallback() != null && !chainDeal.getCallback().equals(BytesUtils.EMPTY_ADDRESS);
    }
}
