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

package com.iexec.sms.authorization;

import static com.iexec.common.chain.ChainTaskStatus.ACTIVE;
import static com.iexec.common.chain.ChainTaskStatus.UNSET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.common.security.Signature;
import com.iexec.common.utils.TestUtils;
import com.iexec.sms.blockchain.IexecHubService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AuthorizationServiceTests {

    @Mock
    IexecHubService iexecHubService;

    @InjectMocks
    private AuthorizationService authorizationService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldBeAuthorizedOnExecutionOfTeeTask() {
        ChainDeal chainDeal = TestUtils.getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        boolean isAuth = authorizationService.isAuthorizedOnExecution(auth, true);
        assertThat(isAuth).isTrue();
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWithNullAuthorization() {
        boolean isAuth = authorizationService.isAuthorizedOnExecution(null, true);
        assertThat(isAuth).isFalse();
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskTypeNotMatchedOnchain() {
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);

        boolean isAuth = authorizationService.isAuthorizedOnExecution(auth, false);
        assertThat(isAuth).isFalse();
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskNotActive() {
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        ChainTask chainTask = TestUtils.getChainTask(UNSET);
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));

        boolean isAuth = authorizationService.isAuthorizedOnExecution(auth, true);
        assertThat(isAuth).isFalse();
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenPoolSignatureIsNotValid() {
        ChainDeal chainDeal = TestUtils.getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        auth.setSignature(new Signature(TestUtils.POOL_WRONG_SIGNATURE));

        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        boolean isAuth = authorizationService.isAuthorizedOnExecution(auth, true);
        assertThat(isAuth).isFalse();
    }

    @Test
    void getChallengeForSetWeb3Secret() {
        String secretAddress = "0x123";
        String secretValue = "ghijk";

        String challenge = authorizationService.getChallengeForSetWeb3Secret(secretAddress, secretValue);
        Assertions.assertEquals("0x8d0b92aaf96f66f172d7615b81f257ebdece2278b7da6c60127cad45852eaaf6",
                challenge);
    }
}