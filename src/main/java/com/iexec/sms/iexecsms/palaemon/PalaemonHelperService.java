package com.iexec.sms.iexecsms.palaemon;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PalaemonHelperService {

    private String PALAEMON_CONFIG_FILE_WITH_DATASET = "src/main/resources/palaemonConfTemplateWithDataset.vm";
    private String PALAEMON_CONFIG_FILE_WITHOUT_DATASET = "src/main/resources/palaemonConfTemplateWithoutDataset.vm";

    private String SESSIONS_ID_PROPERTY = "SESSION_ID";
    private String COMMAND_PROPERTY = "PROPERTY";
    private String MRENCLAVE_PROPERTY = "MRENCLAVE";
    private String FSPF_KEY_PROPERTY = "FSPF_KEY";
    private String FSPF_TAG_PROPERTY = "FSPF_TAG";
    private String ENCLAVE_KEY_PROPERTY = "ENCLAVE_KEY";
    private String TASK_ID_PROPERTY = "TASK_ID";
    private String WORKER_ADDRESS_PROPERTY = "WORKER_ADDRESS";
    private String DATA_FSPF_TAG_PROPERTY = "DATA_FSPF_TAG";
    private String DATA_FSPF_KEY_PROPERTY = "DATA_FSPF_KEY";

    private IexecHubService iexecHubService;

    public PalaemonHelperService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    private Map<String, String> getTokenList(String taskId, String workerAddress) throws Exception {
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

        // The field MREnclave in the smart contract actually contains 3 fields separated by a '|':
        // fspf_key, fspf_tag and MREnclave
        byte[] mrEnclaveBytes = iexecHubService.getAppContract(chainAppId).m_appMREnclave().send();
        String mrEnclaveFull = BytesUtils.bytesToString(mrEnclaveBytes);
        String[] fields = mrEnclaveFull.split("|");
        String fspfKey = fields[0];
        String fspfTag = fields[1];
        String mrEnclave = fields[2];

        Map<String, String> tokens = new HashMap<>();
        tokens.put(SESSIONS_ID_PROPERTY, RandomStringUtils.randomAlphanumeric(10));
        tokens.put(COMMAND_PROPERTY, String.join(",", chainDeal.getParams()));
        tokens.put(MRENCLAVE_PROPERTY, mrEnclave);
        tokens.put(FSPF_KEY_PROPERTY, fspfKey);
        tokens.put(FSPF_TAG_PROPERTY, fspfTag);
        // TODO: ENCLAVE_KEY missing
        tokens.put(TASK_ID_PROPERTY, taskId);
        tokens.put(WORKER_ADDRESS_PROPERTY, workerAddress);
        // TODO: DATASET tokens missing
        return tokens;
    }

    public String getPalaemonConfigurationFile(String taskId, String workerAddress) throws Exception {

        // Palaemon file should be generated and a call to the CAS with this file should happen here.
        Map<String, String> tokens = getTokenList(taskId, workerAddress);

        VelocityEngine ve = new VelocityEngine();
        ve.init();

        Template t;
        if(tokens.containsKey(DATA_FSPF_KEY_PROPERTY) && tokens.containsKey(DATA_FSPF_TAG_PROPERTY)) {
            t = ve.getTemplate(PALAEMON_CONFIG_FILE_WITH_DATASET);
        } else {
            t = ve.getTemplate(PALAEMON_CONFIG_FILE_WITHOUT_DATASET);
        }
        VelocityContext context = new VelocityContext();
        // copy all data from the tokens into context
        tokens.forEach(context::put);

        StringWriter writer = new StringWriter();
        t.merge(context, writer);

        return writer.toString();
    }
}
