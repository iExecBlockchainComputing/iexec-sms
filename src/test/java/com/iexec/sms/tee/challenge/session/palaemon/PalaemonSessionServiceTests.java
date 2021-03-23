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

package com.iexec.sms.tee.challenge.session.palaemon;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.DealParams;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.palaemon.PalaemonSessionRequest;
import com.iexec.sms.tee.session.palaemon.PalaemonSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PalaemonSessionServiceTests {

    @Mock
    private IexecHubService iexecHubService;
    @Mock
    private Web3SecretService web3SecretService;
    @Mock
    private Web2SecretsService web2SecretsService;
    @Mock
    private TeeChallengeService teeChallengeService;

    @InjectMocks
    private PalaemonSessionService palaemonSessionService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void shouldGetCompleteSessionYml() {
        ChainDeal chainDeal = ChainDeal.builder()
                .chainDealId("chainDealId")
                .chainApp(chainApp)
                .chainDataset(chainDataset)
                .requester("requester")
                .params(DealParams.builder()
                        .iexecResultEncryption(true)
                        .iexecArgs("iexecArgs")
                        .iexecResultStorageProvider(null)
                        .iexecResultStorageProxy(null)
                        .iexecTeePostComputeFingerprint("PostComputeFingerprint")
                        .iexecTeePostComputeImage("PostComputeImage")
                        .build())
                .build();
        PalaemonSessionRequest request = PalaemonSessionRequest.builder()
                .taskId("taskId")
                .sessionId("sessionId")
                .workerAddress("workerAddress")
                .chainDeal(chainDeal)
                .enclaveChallenge("enclaveChallenge")
                .build();
    }
}
