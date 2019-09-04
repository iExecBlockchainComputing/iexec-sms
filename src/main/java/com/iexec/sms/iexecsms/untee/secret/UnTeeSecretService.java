package com.iexec.sms.iexecsms.untee.secret;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.sms.secrets.SmsSecret;
import com.iexec.common.sms.secrets.TaskSecrets;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import com.iexec.sms.iexecsms.secret.Secret;
import com.iexec.sms.iexecsms.secret.offchain.OffChainSecrets;
import com.iexec.sms.iexecsms.secret.offchain.OffChainSecretsService;
import com.iexec.sms.iexecsms.secret.onchain.OnChainSecret;
import com.iexec.sms.iexecsms.secret.onchain.OnChainSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UnTeeSecretService {


    private IexecHubService iexecHubService;
    private OnChainSecretService onChainSecretService;
    private OffChainSecretsService offChainSecretsService;

    public UnTeeSecretService(IexecHubService iexecHubService,
                              OnChainSecretService onChainSecretService,
                              OffChainSecretsService offChainSecretsService
    ) {
        this.iexecHubService = iexecHubService;
        this.onChainSecretService = onChainSecretService;
        this.offChainSecretsService = offChainSecretsService;
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

        Optional<OnChainSecret> datasetSecret = onChainSecretService.getSecret(chainDatasetId);
        if (!datasetSecret.isPresent()) {
            log.error("getUnTeeTaskSecrets failed (datasetSecret failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        taskSecretsBuilder.datasetSecret(SmsSecret.builder()
                .address(datasetSecret.get().getAddress())
                .secret(datasetSecret.get().getValue())
                .build());

        Optional<OffChainSecrets> beneficiaryOffChainSecrets = offChainSecretsService.getOffChainSecrets(chainDeal.getBeneficiary());
        if (!beneficiaryOffChainSecrets.isPresent()) {
            log.error("getUnTeeTaskSecrets failed (beneficiaryOffChainSecrets failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        Secret beneficiarySecret = beneficiaryOffChainSecrets.get().getSecret("Kb");
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
