package com.iexec.sms.iexecsms;

import com.iexec.common.chain.ChainDataset;
import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PalaemonHelperService {

    private String SESSIONS_ID_PROPERTY = "SESSION_ID";
    private String COMMAND_PROPERTY = "PROPERTY";
    private String MRENCLAVE_PROPERTY = "MRENCLAVE";
    private String FSPF_KEY_PROPERTY = "FSPF_KEY";
    private String FSPF_TAG_PROPERTY = "FSPF_TAG";
    private String ENCLAVE_KEY_PROPERTY = "ENCLAVE_KEY";
    private String TASK_ID_PROPERTY = "TASK_ID";
    private String WORKER_ADDRESS_PROPERTY = "WORKER_ADDRESS";

    private IexecHubService iexecHubService;

    public PalaemonHelperService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    public Map<String, String> getTokenList(String taskId, String workerAddress) throws Exception {
        Optional<ChainTask> oChainTask = iexecHubService.getChainTask(taskId);
        if(!oChainTask.isPresent()){
            return new HashMap<>();
        }
        ChainTask chainTask = oChainTask.get();
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainTask.getDealid());
        if(!oChainDeal.isPresent()) {
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
        // TODO: COMMAND missing
        tokens.put(MRENCLAVE_PROPERTY, mrEnclave);
        tokens.put(FSPF_KEY_PROPERTY, fspfKey);
        tokens.put(FSPF_TAG_PROPERTY, fspfTag);
        // TODO: ENCLAVE_KEY missing
        tokens.put(TASK_ID_PROPERTY, taskId);
        tokens.put(WORKER_ADDRESS_PROPERTY, workerAddress);
        // TODO: DATASET tokens missing
        return tokens;
    }
}
