package com.iexec.sms.iexecsms.tee.session;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import com.iexec.sms.iexecsms.secret.Secret;
import com.iexec.sms.iexecsms.secret.offchain.OffChainSecrets;
import com.iexec.sms.iexecsms.secret.offchain.OffChainSecretsService;
import com.iexec.sms.iexecsms.secret.onchain.OnChainSecret;
import com.iexec.sms.iexecsms.secret.onchain.OnChainSecretService;
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

@Service
public class TeeSessionHelper {

    //palaemon
    private static final String SESSION_ID_PROPERTY = "SESSION_ID";
    //app
    private static final String APP_MRENCLAVE_PROPERTY = "MRENCLAVE";
    private static final String APP_FSPF_KEY_PROPERTY = "FSPF_KEY";
    private static final String APP_FSPF_TAG_PROPERTY = "FSPF_TAG";
    //data
    private static final String DATASET_FSPF_TAG_PROPERTY = "DATA_FSPF_TAG";
    private static final String DATASET_FSPF_KEY_PROPERTY = "DATA_FSPF_KEY";
    //computing
    private static final String COMMAND_PROPERTY = "COMMAND";
    private static final String TASK_ID_PROPERTY = "TASK_ID";
    private static final String WORKER_ADDRESS_PROPERTY = "WORKER_ADDRESS";
    private static final String ENCLAVE_KEY_PROPERTY = "ENCLAVE_KEY";
    //result encryption
    private static final String BENEFICIARY_KEY_PROPERTY = "BENEFICIARY_KEY";

    private static final String FIELD_SPLITTER = "\\|";

    private TeeSessionConfiguration teeSessionConfiguration;
    private IexecHubService iexecHubService;
    private OnChainSecretService onChainSecretService;
    private OffChainSecretsService offChainSecretsService;
    private TeeChallengeService teeChallengeService;

    public TeeSessionHelper(
            TeeSessionConfiguration teeSessionConfiguration,
            IexecHubService iexecHubService,
            OnChainSecretService onChainSecretService,
            OffChainSecretsService offChainSecretsService,
            TeeChallengeService teeChallengeService) {
        this.teeSessionConfiguration = teeSessionConfiguration;
        this.iexecHubService = iexecHubService;
        this.onChainSecretService = onChainSecretService;
        this.offChainSecretsService = offChainSecretsService;
        this.teeChallengeService = teeChallengeService;
    }

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
        String chainDatasetId = chainDeal.getChainDataset().getChainDatasetId();
        String dealParams = String.join(",", chainDeal.getParams().getIexecArgs());

        //The field MREnclave in the SC contains 3 appFields separated by a '|': fspf_key, fspf_tag & MREnclave
        byte[] appMrEnclaveBytes = iexecHubService.getAppContract(chainAppId).m_appMREnclave().send();
        String appMrEnclaveFull = BytesUtils.hexStringToAscii(BytesUtils.bytesToString(appMrEnclaveBytes));
        String[] appFields = appMrEnclaveFull.split(FIELD_SPLITTER);
        String appFspfKey = appFields[0];
        String appFspfTag = appFields[1];
        String appMrEnclave = appFields[2];

        //TODO: dont use '|' in generic strings (use separate values in db instead)
        //The field symmetricKey in the db contains 2 datasetFields separated by a '|': datasetFspfKey & datasetFspfKey
        Optional<OnChainSecret> datasetSecret = onChainSecretService.getSecret(chainDatasetId);
        String datasetFspfKey = "";
        String datasetFspfTag = "";
        if (datasetSecret.isPresent()) {
            String datasetSecretKey = datasetSecret.get().getValue();
            String[] datasetFields = datasetSecretKey.split(FIELD_SPLITTER);
            datasetFspfKey = datasetFields[0];
            datasetFspfTag = datasetFields[1];
        }

        Optional<TeeChallenge> executionAttestor = teeChallengeService.getOrCreate(taskId);

        Optional<OffChainSecrets> beneficiaryOffChainSecrets = offChainSecretsService.getOffChainSecrets(chainDeal.getBeneficiary());

        String beneficiaryKey = "''";//empty value in yml
        if (!beneficiaryOffChainSecrets.isEmpty()) {
            Secret beneficiaryKeySecret = beneficiaryOffChainSecrets.get().getSecret("Kb");
            beneficiaryKey = beneficiaryKeySecret.getValue();
        }

        Map<String, String> tokens = new HashMap<>();
        //palaemon
        tokens.put(SESSION_ID_PROPERTY, sessionId);
        //app
        tokens.put(APP_MRENCLAVE_PROPERTY, appMrEnclave);
        tokens.put(APP_FSPF_KEY_PROPERTY, appFspfKey);
        tokens.put(APP_FSPF_TAG_PROPERTY, appFspfTag);
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
            tokens.put(ENCLAVE_KEY_PROPERTY, executionAttestor.get().getCredentials().getPrivateKey());
        }
        //result encryption
        tokens.put(BENEFICIARY_KEY_PROPERTY, beneficiaryKey);//base64 encoded by client //TODO deocode in scone runtime app

        return tokens;
    }

    public String getPalaemonConfigurationFile(String sessionId, String taskId, String workerAddress, String attestingEnclave) throws Exception {
        // Palaemon file should be generated and a call to the CAS with this file should happen here.
        Map<String, String> tokens = getTokenList(sessionId, taskId, workerAddress, attestingEnclave);

        VelocityEngine ve = new VelocityEngine();
        ve.init();

        Template t;
        if (tokens.containsKey(DATASET_FSPF_KEY_PROPERTY) && tokens.containsKey(DATASET_FSPF_TAG_PROPERTY)) {
            t = ve.getTemplate(teeSessionConfiguration.getPalaemonConfigFileWithDataset());
        } else {
            t = ve.getTemplate(teeSessionConfiguration.getPalaemonConfigFileWithoutDataset());
        }
        VelocityContext context = new VelocityContext();
        // copy all data from the tokens into context
        tokens.forEach(context::put);

        StringWriter writer = new StringWriter();
        t.merge(context, writer);

        return writer.toString();
    }
}
