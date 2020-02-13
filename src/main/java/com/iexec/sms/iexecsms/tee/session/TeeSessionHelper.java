package com.iexec.sms.iexecsms.tee.session;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import com.iexec.sms.iexecsms.secret.Secret;
import com.iexec.sms.iexecsms.secret.web2.Web2Secrets;
import com.iexec.sms.iexecsms.secret.web2.Web2SecretsService;
import com.iexec.sms.iexecsms.secret.web3.Web3Secret;
import com.iexec.sms.iexecsms.secret.web3.Web3SecretService;
import com.iexec.sms.iexecsms.tee.challenge.TeeChallenge;
import com.iexec.sms.iexecsms.tee.challenge.TeeChallengeService;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.iexec.sms.iexecsms.secret.ReservedSecretKeyName.IEXEC_RESULT_DROPBOX_TOKEN;
import static com.iexec.sms.iexecsms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;

@Service
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
    private static final String IEXEC_REQUESTER_STORAGE_LOCATION_PROPERTY = "IEXEC_REQUESTER_STORAGE_LOCATION";
    private static final String REQUESTER_DROPBOX_TOKEN_PROPERTY = "REQUESTER_DROPBOX_TOKEN";


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

        // Post-compute
        String postComputeMrEnclaveFull = teeSessionHelperConfiguration.getSconeTeePostComputeMrEnclave();
        String[] postComputeFields = postComputeMrEnclaveFull.split(FIELD_SPLITTER);
        String postComputeFspfKey = postComputeFields[0];
        String postComputeFspfTag = postComputeFields[1];
        String postComputeMrEnclave = postComputeFields[2];

        // Dataset (optional)
        String datasetFspfKey = "";
        String datasetFspfTag = "";
        if (chainDeal.getChainDataset() != null){
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
            if (beneficiaryResultEncryptionKeySecret!= null){
                beneficiaryResultEncryptionKey = beneficiaryResultEncryptionKeySecret.getValue();
            }
        }

        // storage
        // TODO: We need a signature of the beneficiary to push to the beneficiary private storage space
        //  waiting for that feature we only allow to push to the requester private storage space
        Optional<Web2Secrets> requesterSecrets = web2SecretsService.getWeb2Secrets(chainDeal.getRequester(), true);
        String storageLocation = chainDeal.getParams().getIexecResultStorageProvider();
        String resultEncryption = chainDeal.getParams().getIexecResultEncryption();
        //TODO: Generify beneficiary secret retrieval & templating
        String requesterResultDropboxToken = "''";//empty value in yml
        if (!requesterSecrets.isEmpty()) {
            Secret requesterResultDropboxTokenSecret = requesterSecrets.get().getSecret(IEXEC_RESULT_DROPBOX_TOKEN);
            if (requesterResultDropboxTokenSecret!= null){
                requesterResultDropboxToken = requesterResultDropboxTokenSecret.getValue();
            }
        }

        Map<String, String> tokens = new HashMap<>();
        //palaemon
        tokens.put(SESSION_ID_PROPERTY, sessionId);
        //app
        tokens.put(APP_FSPF_KEY_PROPERTY, appFspfKey);
        tokens.put(APP_FSPF_TAG_PROPERTY, appFspfTag);
        tokens.put(APP_MRENCLAVE_PROPERTY, appMrEnclave);
        //post-compute
        tokens.put(POST_COMPUTE_FSPF_KEY_PROPERTY, postComputeFspfKey);
        tokens.put(POST_COMPUTE_FSPF_TAG_PROPERTY, postComputeFspfTag);
        tokens.put(POST_COMPUTE_MRENCLAVE_PROPERTY, postComputeMrEnclave);
        //data
        if (!datasetFspfKey.isEmpty()) {
            tokens.put(DATASET_FSPF_KEY_PROPERTY, datasetFspfKey);
        }
        if (!datasetFspfTag.isEmpty()) {
            tokens.put(DATASET_FSPF_TAG_PROPERTY, datasetFspfTag);
        }
        //computing
        tokens.put(COMMAND_PROPERTY, dealParams);
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
        tokens.put(IEXEC_REQUESTER_STORAGE_LOCATION_PROPERTY, storageLocation);
        if (requesterResultDropboxToken != null && !requesterResultDropboxToken.isEmpty()) {
            tokens.put(REQUESTER_DROPBOX_TOKEN_PROPERTY, requesterResultDropboxToken);
        }

        return tokens;
    }


    public String getPalaemonConfigurationFile(String sessionId, String taskId, String workerAddress, String attestingEnclave) throws Exception {
        // Palaemon file should be generated and a call to the CAS with this file should happen here.
        Map<String, String> tokens = getTokenList(sessionId, taskId, workerAddress, attestingEnclave);

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
