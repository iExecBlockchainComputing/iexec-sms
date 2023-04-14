/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.chain.ChainDeal;
import com.iexec.commons.poco.chain.ChainTask;
import com.iexec.commons.poco.chain.WorkerpoolAuthorization;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.utils.TestUtils;
import com.iexec.sms.blockchain.IexecHubService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static com.iexec.commons.poco.chain.ChainTaskStatus.ACTIVE;
import static com.iexec.commons.poco.chain.ChainTaskStatus.UNSET;
import static com.iexec.sms.authorization.AuthorizationError.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AuthorizationServiceTests {

    @Mock
    IexecHubService iexecHubService;

    @InjectMocks
    private AuthorizationService authorizationService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region isAuthorizedOnExecution
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
    // endregion

    // region isAuthorizedOnExecutionWithDetailedIssue
    @Test
    void shouldBeAuthorizedOnExecutionOfTeeTaskWithDetails() {
        ChainDeal chainDeal = TestUtils.getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
        assertThat(isAuth).isEmpty();
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWithNullAuthorizationWithDetails() {
        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(null, true);
        assertThat(isAuth).isNotEmpty()
                .get()
                .isEqualTo(EMPTY_PARAMS_UNAUTHORIZED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskTypeNotMatchedOnchainWithDetails() {
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, false);
                assertThat(isAuth).isNotEmpty()
                .get()
                .isEqualTo(NO_MATCH_ONCHAIN_TYPE);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenGetTaskFailedWithDetails() {
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.empty());

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
                assertThat(isAuth).isNotEmpty()
                .get()
                .isEqualTo(GET_CHAIN_TASK_FAILED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskNotActiveWithDetails() {
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        ChainTask chainTask = TestUtils.getChainTask(UNSET);
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
                assertThat(isAuth).isNotEmpty()
                .get()
                .isEqualTo(TASK_NOT_ACTIVE);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenGetDealFailedWithDetails() {
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        auth.setSignature(new Signature(TestUtils.POOL_WRONG_SIGNATURE));

        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.empty());

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
                assertThat(isAuth).isNotEmpty()
                .get()
                .isEqualTo(GET_CHAIN_DEAL_FAILED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenPoolSignatureIsNotValidWithDetails() {
        ChainDeal chainDeal = TestUtils.getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        auth.setSignature(new Signature(TestUtils.POOL_WRONG_SIGNATURE));

        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
                assertThat(isAuth).isNotEmpty()
                .get()
                .isEqualTo(INVALID_SIGNATURE);
    }
    // endregion

    @Test
    void getChallengeForSetRequesterAppComputeSecret() {
        String challenge = authorizationService.getChallengeForSetRequesterAppComputeSecret(
                "", "0", "");
        Assertions.assertEquals("0x31991eefc2731228bdd25dbc5a242722eda3869f9b06536dbd96a774e5228509",
                challenge);
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