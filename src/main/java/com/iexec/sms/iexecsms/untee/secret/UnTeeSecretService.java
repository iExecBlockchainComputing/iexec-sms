package com.iexec.sms.iexecsms.untee.secret;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.sms.secrets.SmsSecret;
import com.iexec.common.sms.secrets.TaskSecrets;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import com.iexec.sms.iexecsms.secret.ReservedSecretKeyName;
import com.iexec.sms.iexecsms.secret.Secret;
import com.iexec.sms.iexecsms.secret.web2.Web2Secrets;
import com.iexec.sms.iexecsms.secret.web2.Web2SecretsService;
import com.iexec.sms.iexecsms.secret.web3.Web3Secret;
import com.iexec.sms.iexecsms.secret.web3.Web3SecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.iexec.sms.iexecsms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;

@Service
@Slf4j
public class UnTeeSecretService {


    private IexecHubService iexecHubService;
    private Web3SecretService web3SecretService;
    private Web2SecretsService web2SecretsService;

    public UnTeeSecretService(IexecHubService iexecHubService,
                              Web3SecretService web3SecretService,
                              Web2SecretsService web2SecretsService
    ) {
        this.iexecHubService = iexecHubService;
        this.web3SecretService = web3SecretService;
        this.web2SecretsService = web2SecretsService;
    }

    /*
    *
    * Untested yet
    *
    * */
    public Optional<TaskSecrets> getUnTeeTaskSecrets(String chainTaskId) {
        TaskSecrets.TaskSecretsBuilder taskSecretsBuilder = TaskSecrets.builder();

        Optional<ChainTask> oChainTask = iexecHubService.getChainTask(chainTaskId);
        if (!oChainTask.isPresent()) {
            log.error("getUnTeeTaskSecrets failed (getChainTask failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        ChainTask chainTask = oChainTask.get();
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainTask.getDealid());
        if (!oChainDeal.isPresent()) {
            log.error("getUnTeeTaskSecrets failed (getChainDeal failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        ChainDeal chainDeal = oChainDeal.get();
        String chainDatasetId = chainDeal.getChainDataset().getChainDatasetId();

        Optional<Web3Secret> datasetSecret = web3SecretService.getSecret(chainDatasetId, true);
        if (!datasetSecret.isPresent()) {
            log.error("getUnTeeTaskSecrets failed (datasetSecret failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        taskSecretsBuilder.datasetSecret(SmsSecret.builder()
                .address(datasetSecret.get().getAddress())
                .secret(datasetSecret.get().getValue())
                .build());

        Optional<Web2Secrets> beneficiarySecrets = web2SecretsService.getWeb2Secrets(chainDeal.getBeneficiary(), true);
        if (!beneficiarySecrets.isPresent()) {
            log.error("getUnTeeTaskSecrets failed (beneficiarySecrets failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        Secret beneficiarySecret = beneficiarySecrets.get().getSecret(IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY);
        if (beneficiarySecret == null) {
            log.error("getUnTeeTaskSecrets failed (beneficiarySecret empty) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        taskSecretsBuilder.beneficiarySecret(SmsSecret.builder()
                .address(beneficiarySecret.getAddress())
                .secret(beneficiarySecret.getValue())
                .build());

        return Optional.of(taskSecretsBuilder.build());
    }


}
