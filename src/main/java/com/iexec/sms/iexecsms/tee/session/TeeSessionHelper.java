package com.iexec.sms.iexecsms.tee.session;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import com.iexec.sms.iexecsms.secret.Secret;
import com.iexec.sms.iexecsms.secret.web2.Web2Secrets;
import com.iexec.sms.iexecsms.secret.web2.Web2SecretsService;
import com.iexec.sms.iexecsms.secret.web3.Web3Secret;
import com.iexec.sms.iexecsms.secret.web3.Web3SecretService;
import com.iexec.sms.iexecsms.tee.challenge.TeeChallenge;
import com.iexec.sms.iexecsms.tee.challenge.TeeChallengeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.iexec.sms.iexecsms.secret.ReservedSecretKeyName.*;

@Service
@Slf4j
public class TeeSessionHelper {

    //TODO - prefix all envvars by IEXEC_*
    //palaemon
    private static final String SESSION_ID_PROPERTY = "SESSION_ID";
    //app
    private static final String APP_FSPF_KEY_PROPERTY = "APP_FSPF_KEY";
    private static final String APP_FSPF_TAG_PROPERTY = "APP_FSPF_TAG";
    private static final String APP_MRENCLAVE_PROPERTY = "APP_MRENCLAVE";
    //post-compute
    private static final String POST_COMPUTE_FSPF_KEY_PROPERTY = "POST_COMPUTE_FSPF_KEY";
    private static final String POST_COMPUTE_FSPF_TAG_PROPERTY = "POST_COMPUTE_FSPF_TAG";
    private static final String POST_COMPUTE_MRENCLAVE_PROPERTY = "POST_COMPUTE_MRENCLAVE";
    //data
    private static final String DATASET_FSPF_KEY_PROPERTY = "DATA_FSPF_KEY";
    private static final String DATASET_FSPF_TAG_PROPERTY = "DATA_FSPF_TAG";
    //computing
    private static final String COMMAND_PROPERTY = "COMMAND";
    private static final String TASK_ID_PROPERTY = "TASK_ID";
    private static final String WORKER_ADDRESS_PROPERTY = "WORKER_ADDRESS";
    private static final String TEE_CHALLENGE_PRIVATE_KEY_PROPERTY = "TEE_CHALLENGE_PRIVATE_KEY";
    // encryption
    private static final String IEXEC_REQUESTER_RESULT_ENCRYPTION_PROPERTY = "IEXEC_REQUESTER_RESULT_ENCRYPTION";
    private static final String BENEFICIARY_PUBLIC_KEY_BASE64_PROPERTY = "BENEFICIARY_PUBLIC_KEY_BASE64";
    //storage
    private static final String IEXEC_REQUESTER_SHOULD_CALLBACK_PROPERTY = "IEXEC_REQUESTER_SHOULD_CALLBACK";
    private static final String IEXEC_REQUESTER_STORAGE_LOCATION_PROPERTY = "IEXEC_REQUESTER_STORAGE_LOCATION";//TODO rename to storage_provider
    private static final String IEXEC_REQUESTER_STORAGE_PROXY_PROPERTY = "IEXEC_REQUESTER_STORAGE_PROXY";
    private static final String REQUESTER_STORAGE_TOKEN_PROPERTY = "REQUESTER_STORAGE_TOKEN";


    private static final String FIELD_SPLITTER = "\\|";

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

    /*
     * Nb: MREnclave from request param contains 3 appFields separated by a '|': fspf_key, fspf_tag & MREnclave
     * */
    public Map<String, String> getTokenList(String sessionId, String taskId, String workerAddress, String attestingEnclave) throws Exception {
        Optional<ChainTask> oChainTask = iexecHubService.getChainTask(taskId);
        if (!oChainTask.isPresent()) {
            throw new RuntimeException("No chain task retrieved");
        }
        ChainTask chainTask = oChainTask.get();
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainTask.getDealid());
        if (!oChainDeal.isPresent()) {
            throw new RuntimeException("No chain deal retrieved");
        }
        ChainDeal chainDeal = oChainDeal.get();
        String chainAppId = chainDeal.getChainApp().getChainAppId();

        String dealParams = String.join(",", chainDeal.getParams().getIexecArgs());

        // App
        TeeAppFingerprint appFingerprint = getAppFingerprint(chainAppId);

        // Post-compute
        TeeAppFingerprint postComputeFingerprint = getPostComputeFingerprint();

        // Dataset (optional)
        String datasetFspfKey = "";
        String datasetFspfTag = "";
        if (chainDeal.getChainDataset() != null && !chainDeal.getChainDataset().getChainDatasetId().equals(BytesUtils.EMPTY_ADDRESS)) {
            String chainDatasetId = chainDeal.getChainDataset().getChainDatasetId();
            Optional<Web3Secret> datasetSecret = web3SecretService.getSecret(chainDatasetId, true);

            if (datasetSecret.isPresent()) {
                String datasetSecretKey = datasetSecret.get().getValue();
                String[] datasetFields = datasetSecretKey.split(FIELD_SPLITTER);
                datasetFspfKey = datasetFields[0];
                datasetFspfTag = datasetFields[1];
            }
        }

        //encryption
        Optional<TeeChallenge> executionAttestor = teeChallengeService.getOrCreate(taskId, true);
        Optional<Web2Secrets> beneficiarySecrets = web2SecretsService.getWeb2Secrets(chainDeal.getBeneficiary(), true);
        String beneficiaryResultEncryptionKey = "''";//empty value in yml
        if (!beneficiarySecrets.isEmpty()) {
            Secret beneficiaryResultEncryptionKeySecret = beneficiarySecrets.get().getSecret(IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY);
            if (beneficiaryResultEncryptionKeySecret != null) {
                beneficiaryResultEncryptionKey = beneficiaryResultEncryptionKeySecret.getValue();
            }
        }

        // storage
        // TODO: We need a signature of the beneficiary to push to the beneficiary private storage space
        //  waiting for that feature we only allow to push to the requester private storage space
        Optional<Web2Secrets> requesterSecrets = web2SecretsService.getWeb2Secrets(chainDeal.getRequester(), true);
        String shouldCallback = "no"; //CAS does not accept boolean in yml (Failed to generateSecureSession)
        if (chainDeal.getCallback() != null && !chainDeal.getCallback().equals(BytesUtils.EMPTY_ADDRESS)){
            shouldCallback = "yes";
        }
        String storageLocation = chainDeal.getParams().getIexecResultStorageProvider();
        String resultEncryption = chainDeal.getParams().getIexecResultEncryption();
        String requesterStorageToken = "''";//empty value in yml
        String storageProxy = "''";
        if (shouldCallback.equals("no")){
            //TODO: Generify beneficiary secret retrieval & templating
            Secret requesterStorageTokenSecret;
            if (!requesterSecrets.isEmpty()) {
                switch (storageLocation){
                    case "dropbox":
                        requesterStorageTokenSecret = requesterSecrets.get().getSecret(IEXEC_RESULT_DROPBOX_TOKEN);
                        break;
                    case "ipfs":
                    default:
                        requesterStorageTokenSecret = requesterSecrets.get().getSecret(IEXEC_RESULT_IEXEC_IPFS_TOKEN);
                        break;
                }
                if (requesterStorageTokenSecret != null) {
                    requesterStorageToken = requesterStorageTokenSecret.getValue();
                }
            }
            Optional<TaskDescription> taskDescription = iexecHubService.getTaskDescriptionFromChain(taskId);
            if (taskDescription.isPresent()){
                storageProxy = taskDescription.get().getResultStorageProxy();
            }
        }

        Map<String, String> tokens = new HashMap<>();
        //palaemon
        tokens.put(SESSION_ID_PROPERTY, sessionId);
        //app
        tokens.put(APP_FSPF_KEY_PROPERTY, appFingerprint.getFspfKey());
        tokens.put(APP_FSPF_TAG_PROPERTY, appFingerprint.getFspfTag());
        tokens.put(APP_MRENCLAVE_PROPERTY, appFingerprint.getMrEnclave());
        //post-compute
        tokens.put(POST_COMPUTE_FSPF_KEY_PROPERTY, postComputeFingerprint.getFspfKey());
        tokens.put(POST_COMPUTE_FSPF_TAG_PROPERTY, postComputeFingerprint.getFspfTag());
        tokens.put(POST_COMPUTE_MRENCLAVE_PROPERTY, postComputeFingerprint.getMrEnclave());
        //data
        if (!datasetFspfKey.isEmpty()) {
            tokens.put(DATASET_FSPF_KEY_PROPERTY, datasetFspfKey);
        }
        if (!datasetFspfTag.isEmpty()) {
            tokens.put(DATASET_FSPF_TAG_PROPERTY, datasetFspfTag);
        }
        //computing
        String command = dealParams.isEmpty() ? appFingerprint.getEntrypoint() : appFingerprint.getEntrypoint() + " " + dealParams;
        tokens.put(COMMAND_PROPERTY, command);
        tokens.put(TASK_ID_PROPERTY, taskId);
        tokens.put(WORKER_ADDRESS_PROPERTY, workerAddress);
        if (!attestingEnclave.isEmpty() && executionAttestor.isPresent()
                && executionAttestor.get().getCredentials().getPrivateKey() != null) {
            tokens.put(TEE_CHALLENGE_PRIVATE_KEY_PROPERTY, executionAttestor.get().getCredentials().getPrivateKey());
        }
        //encryption
        tokens.put(IEXEC_REQUESTER_RESULT_ENCRYPTION_PROPERTY, resultEncryption);//TODO read that onchain from enclave instead?
        tokens.put(BENEFICIARY_PUBLIC_KEY_BASE64_PROPERTY, beneficiaryResultEncryptionKey);//base64 encoded by client

        //storage
        tokens.put(IEXEC_REQUESTER_SHOULD_CALLBACK_PROPERTY, shouldCallback);
        tokens.put(IEXEC_REQUESTER_STORAGE_LOCATION_PROPERTY, storageLocation);
        tokens.put(IEXEC_REQUESTER_STORAGE_PROXY_PROPERTY, storageProxy);
        if (requesterStorageToken != null && !requesterStorageToken.isEmpty()) {
            tokens.put(REQUESTER_STORAGE_TOKEN_PROPERTY, requesterStorageToken);
        }

        return tokens;
    }

    TeeAppFingerprint getAppFingerprint(String chainId) throws Exception {
        byte[] appFingerprintBytes = iexecHubService.getAppContract(chainId).m_appMREnclave().send();
        String appFingerprintString = BytesUtils.hexStringToAscii(BytesUtils.bytesToString(appFingerprintBytes));
        return new TeeAppFingerprint(appFingerprintString, FIELD_SPLITTER);
    }

    TeeAppFingerprint getPostComputeFingerprint() {
        String postComputeMrEnclaveFull = teeSessionHelperConfiguration.getSconeTeePostComputeMrEnclave();
        return new TeeAppFingerprint(postComputeMrEnclaveFull, FIELD_SPLITTER);
    }

    public String getPalaemonConfigurationFile(String sessionId, String taskId, String workerAddress, String attestingEnclave) {
        // Palaemon file should be generated and a call to the CAS with this file should happen here.
        Map<String, String> tokens;

        try {
            tokens = getTokenList(sessionId, taskId, workerAddress, attestingEnclave);
        } catch (Exception e) {
            return "";
        }

        VelocityEngine ve = new VelocityEngine();
        ve.init();

        Template t;
        if (tokens.containsKey(DATASET_FSPF_KEY_PROPERTY) && tokens.containsKey(DATASET_FSPF_TAG_PROPERTY)) {
            t = ve.getTemplate(teeSessionHelperConfiguration.getPalaemonConfigFileWithDataset());
        } else {
            t = ve.getTemplate(teeSessionHelperConfiguration.getPalaemonConfigFileWithoutDataset());
        }
        VelocityContext context = new VelocityContext();
        // copy all data from the tokens into context
        tokens.forEach(context::put);

        StringWriter writer = new StringWriter();
        t.merge(context, writer);

        return writer.toString();
    }
}
