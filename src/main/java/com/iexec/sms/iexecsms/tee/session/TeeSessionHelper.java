package com.iexec.sms.iexecsms.tee.session;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import com.iexec.sms.iexecsms.secret.ReservedSecretKeyName;
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

    //palaemon
    private static final String SESSION_ID_PROPERTY = "SESSION_ID";
    //app
    private static final String APP_FSPF_KEY = "APP_FSPF_KEY";
    private static final String APP_FSPF_TAG = "APP_FSPF_TAG";
    private static final String APP_MRENCLAVE = "APP_MRENCLAVE";
    private static final String IEXEC_ARGS = "IEXEC_ARGS";

    //data
    private static final String DATASET_FSPF_KEY = "DATA_FSPF_KEY";
    private static final String DATASET_FSPF_TAG = "DATA_FSPF_TAG";

    //post-compute
    private static final String POST_COMPUTE_FSPF_KEY = "POST_COMPUTE_FSPF_KEY";
    private static final String POST_COMPUTE_FSPF_TAG = "POST_COMPUTE_FSPF_TAG";
    private static final String POST_COMPUTE_MRENCLAVE = "POST_COMPUTE_MRENCLAVE";

    // result encryption
    private static final String IEXEC_RESULT_ENCRYPTION = "IEXEC_RESULT_ENCRYPTION";
    private static final String IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY = "IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY";//BASE64
    //result storage
    private static final String IEXEC_RESULT_STORAGE_PROVIDER = "IEXEC_RESULT_STORAGE_PROVIDER";
    private static final String IEXEC_RESULT_STORAGE_PROXY = "IEXEC_RESULT_STORAGE_PROXY";
    private static final String IEXEC_RESULT_STORAGE_TOKEN = "IEXEC_RESULT_STORAGE_TOKEN";
    private static final String IEXEC_RESULT_STORAGE_CALLBACK = "IEXEC_RESULT_STORAGE_CALLBACK";
    //result sign
    private static final String IEXEC_RESULT_SIGN_TASK_ID = "IEXEC_RESULT_SIGN_TASK_ID";
    private static final String IEXEC_RESULT_SIGN_WORKER_ADDRESS = "IEXEC_RESULT_SIGN_WORKER_ADDRESS";
    private static final String IEXEC_RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY = "IEXEC_RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY";


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
            return new HashMap<>();
        }
        ChainTask chainTask = oChainTask.get();
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainTask.getDealid());
        if (!oChainDeal.isPresent()) {
            return new HashMap<>();
        }
        ChainDeal chainDeal = oChainDeal.get();
        String chainAppId = chainDeal.getChainApp().getChainAppId();

        String dealParams = String.join(",", chainDeal.getParams().getIexecArgs());

        // App
        byte[] appMrEnclaveBytes = iexecHubService.getAppContract(chainAppId).m_appMREnclave().send();
        String appMrEnclaveFull = BytesUtils.hexStringToAscii(BytesUtils.bytesToString(appMrEnclaveBytes));
        String[] appFields = appMrEnclaveFull.split(FIELD_SPLITTER);
        String appFspfKey = appFields[0];
        String appFspfTag = appFields[1];
        String appMrEnclave = appFields[2];
        String appEntrypoint = appFields[3];

        // Post-compute
        String postComputeMrEnclaveFull = teeSessionHelperConfiguration.getSconeTeePostComputeMrEnclave();
        String[] postComputeFields = postComputeMrEnclaveFull.split(FIELD_SPLITTER);
        String postComputeFspfKey = postComputeFields[0];
        String postComputeFspfTag = postComputeFields[1];
        String postComputeMrEnclave = postComputeFields[2];

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
            Secret beneficiaryResultEncryptionKeySecret = beneficiarySecrets.get().getSecret(ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY);
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
                        requesterStorageTokenSecret = requesterSecrets.get().getSecret(ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN);
                        break;
                    case "ipfs":
                    default:
                        requesterStorageTokenSecret = requesterSecrets.get().getSecret(ReservedSecretKeyName.IEXEC_RESULT_IEXEC_IPFS_TOKEN);
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
        tokens.put(APP_FSPF_KEY, appFspfKey);
        tokens.put(APP_FSPF_TAG, appFspfTag);
        tokens.put(APP_MRENCLAVE, appMrEnclave);
        //post-compute
        tokens.put(POST_COMPUTE_FSPF_KEY, postComputeFspfKey);
        tokens.put(POST_COMPUTE_FSPF_TAG, postComputeFspfTag);
        tokens.put(POST_COMPUTE_MRENCLAVE, postComputeMrEnclave);
        //data
        if (!datasetFspfKey.isEmpty()) {
            tokens.put(DATASET_FSPF_KEY, datasetFspfKey);
        }
        if (!datasetFspfTag.isEmpty()) {
            tokens.put(DATASET_FSPF_TAG, datasetFspfTag);
        }
        //computing
        String command = appEntrypoint;
        if (!dealParams.isEmpty()) {
            command = appEntrypoint + " " + dealParams;
        }
        tokens.put(IEXEC_ARGS, command);
        tokens.put(IEXEC_RESULT_SIGN_TASK_ID, taskId);
        tokens.put(IEXEC_RESULT_SIGN_WORKER_ADDRESS, workerAddress);
        if (!attestingEnclave.isEmpty() && executionAttestor.isPresent()
                && executionAttestor.get().getCredentials().getPrivateKey() != null) {
            tokens.put(IEXEC_RESULT_SIGN_TEE_CHALLENGE_PRIVATE_KEY, executionAttestor.get().getCredentials().getPrivateKey());
        }
        //encryption
        tokens.put(IEXEC_RESULT_ENCRYPTION, resultEncryption);//TODO read that onchain from enclave instead?
        tokens.put(IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY, beneficiaryResultEncryptionKey);//base64 encoded by client

        //storage
        tokens.put(IEXEC_RESULT_STORAGE_CALLBACK, shouldCallback);
        tokens.put(IEXEC_RESULT_STORAGE_PROVIDER, storageLocation);
        tokens.put(IEXEC_RESULT_STORAGE_PROXY, storageProxy);
        if (requesterStorageToken != null && !requesterStorageToken.isEmpty()) {
            tokens.put(IEXEC_RESULT_STORAGE_TOKEN, requesterStorageToken);
        }

        return tokens;
    }


    public String getPalaemonConfigurationFile(String sessionId, String taskId, String workerAddress, String attestingEnclave) throws Exception {
        // Palaemon file should be generated and a call to the CAS with this file should happen here.
        Map<String, String> tokens = getTokenList(sessionId, taskId, workerAddress, attestingEnclave);

        VelocityEngine ve = new VelocityEngine();
        ve.init();

        Template t;
        if (tokens.containsKey(DATASET_FSPF_KEY) && tokens.containsKey(DATASET_FSPF_TAG)) {
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
