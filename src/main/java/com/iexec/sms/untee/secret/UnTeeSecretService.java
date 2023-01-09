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

package com.iexec.sms.untee.secret;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.sms.secret.SmsSecret;
import com.iexec.common.sms.secret.TaskSecrets;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.secret.Secret;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.iexec.sms.secret.ReservedSecretKeyName.IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY;

@Service
@Slf4j
public class UnTeeSecretService {

    private final IexecHubService iexecHubService;
    private final Web3SecretService web3SecretService;
    private final Web2SecretsService web2SecretsService;

    public UnTeeSecretService(IexecHubService iexecHubService,
                              Web3SecretService web3SecretService,
                              Web2SecretsService web2SecretsService
    ) {
        this.iexecHubService = iexecHubService;
        this.web3SecretService = web3SecretService;
        this.web2SecretsService = web2SecretsService;
    }

    /*
     * Untested yet
     */
    public Optional<TaskSecrets> getUnTeeTaskSecrets(String chainTaskId) {
        TaskSecrets.TaskSecretsBuilder taskSecretsBuilder = TaskSecrets.builder();

        // TODO use taskDescription instead of chainDeal
        Optional<ChainTask> oChainTask = iexecHubService.getChainTask(chainTaskId);
        if (oChainTask.isEmpty()) {
            log.error("getUnTeeTaskSecrets failed (getChainTask failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        ChainTask chainTask = oChainTask.get();
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainTask.getDealid());
        if (oChainDeal.isEmpty()) {
            log.error("getUnTeeTaskSecrets failed (getChainDeal failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        ChainDeal chainDeal = oChainDeal.get();
        String chainDatasetId = chainDeal.getChainDataset().getChainDatasetId();

        Optional<Web3Secret> datasetSecret = web3SecretService.getSecret(chainDatasetId, true);
        if (datasetSecret.isEmpty()) {
            log.error("getUnTeeTaskSecrets failed (datasetSecret failed) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        taskSecretsBuilder.datasetSecret(SmsSecret.builder()
                .address(datasetSecret.get().getAddress())
                .secret(datasetSecret.get().getValue())
                .build());

        Optional<Secret> beneficiarySecret = web2SecretsService.getSecret(chainDeal.getBeneficiary(),
                IEXEC_RESULT_ENCRYPTION_PUBLIC_KEY, true);
        if (beneficiarySecret.isEmpty()) {
            log.error("getUnTeeTaskSecrets failed (beneficiarySecret empty) [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        taskSecretsBuilder.beneficiarySecret(SmsSecret.builder()
                .address(beneficiarySecret.get().getAddress())
                .secret(beneficiarySecret.get().getValue())
                .build());

        return Optional.of(taskSecretsBuilder.build());
    }

}
