/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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
import com.iexec.commons.poco.tee.TeeUtils;
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

    // region isAuthorizedOnExecutionWithDetailedIssue
    @Test
    void shouldBeAuthorizedOnExecutionOfTeeTaskWithDetails() {
        ChainDeal chainDeal = getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
        assertThat(isAuth).isEmpty();
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWithNullAuthorizationWithDetails() {
        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(null, true);
        assertThat(isAuth).isNotEmpty()
                .contains(EMPTY_PARAMS_UNAUTHORIZED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWithEmptyAuthorizationWithDetails() {
        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(null, true);
        assertThat(isAuth).isNotEmpty()
                .contains(EMPTY_PARAMS_UNAUTHORIZED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskTypeNotMatchedOnchainWithDetails() {
        ChainDeal chainDeal = getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, false);
        assertThat(isAuth).isNotEmpty()
                .contains(NO_MATCH_ONCHAIN_TYPE);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenGetTaskFailedWithDetails() {
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.empty());

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
        assertThat(isAuth).isNotEmpty()
                .contains(GET_CHAIN_TASK_FAILED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskNotActiveWithDetails() {
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        ChainTask chainTask = TestUtils.getChainTask(UNSET);
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
        assertThat(isAuth).isNotEmpty()
                .contains(TASK_NOT_ACTIVE);
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
                .contains(GET_CHAIN_DEAL_FAILED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenPoolSignatureIsNotValidWithDetails() {
        ChainDeal chainDeal = getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        WorkerpoolAuthorization auth = TestUtils.getTeeWorkerpoolAuth();
        auth.setSignature(new Signature(TestUtils.POOL_WRONG_SIGNATURE));

        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth, true);
        assertThat(isAuth).isNotEmpty()
                .contains(INVALID_SIGNATURE);
    }
    // endregion

    // region challenges
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

    @Test
    void getChallengeForWorker() {
        final WorkerpoolAuthorization authorization = WorkerpoolAuthorization.builder()
                .chainTaskId("0x0123")
                .enclaveChallenge("0x4567")
                .workerWallet("0xabcd")
                .build();
        final String challenge = authorizationService.getChallengeForWorker(authorization);
        assertThat(challenge).isEqualTo("0x0a17b60a69e733c4199912dc3c5bfd4b17aa6bcfbf3cfbfe6230f00e21f96b85");
    }
    // endregion

    // region utils
    ChainDeal getChainDeal() {
        return ChainDeal.builder()
                .poolOwner("0xc911f9345717ba7c8ec862ce002af3e058df84e4")
                .tag(TeeUtils.TEE_SCONE_ONLY_TAG)
                .build();
    }
    // endregion
}
