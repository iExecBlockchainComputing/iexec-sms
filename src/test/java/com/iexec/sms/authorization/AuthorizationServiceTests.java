/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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
import com.iexec.commons.poco.chain.ChainTaskStatus;
import com.iexec.commons.poco.chain.WorkerpoolAuthorization;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.tee.TeeUtils;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.HashUtils;
import com.iexec.commons.poco.utils.SignatureUtils;
import com.iexec.sms.chain.IexecHubService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.iexec.commons.poco.chain.ChainTaskStatus.ACTIVE;
import static com.iexec.commons.poco.chain.ChainTaskStatus.UNSET;
import static com.iexec.sms.authorization.AuthorizationError.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTests {

    private static final String ENCLAVE_ADDRESS = "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
    private static final String DEAL_ID = "0x2222222222222222222222222222222222222222222222222222222222222222";
    private static final String WORKER_ADDRESS = "0x87ae2b87b5db23830572988fb1f51242fbc471ce";
    private static final String CHAIN_TASK_ID = "0x1111111111111111111111111111111111111111111111111111111111111111";
    private static final String POOL_PRIVATE = "0xe2a973b083fae8043543f15313955aecee9de809a318656c1cfb22d3a6d52de1";

    @Mock
    IexecHubService iexecHubService;

    @InjectMocks
    private AuthorizationService authorizationService;

    // region isAuthorizedOnExecutionWithDetailedIssue
    @Test
    void shouldBeAuthorizedOnExecutionOfTeeTaskWithDetails() {
        final ChainDeal chainDeal = getChainDeal();
        final ChainTask chainTask = getChainTask(ACTIVE);
        final WorkerpoolAuthorization auth = getTeeWorkerpoolAuth();
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        final Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth);
        assertThat(isAuth).isEmpty();
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWithNullAuthorizationWithDetails() {
        final Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(null);
        assertThat(isAuth).isNotEmpty()
                .contains(EMPTY_PARAMS_UNAUTHORIZED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWithEmptyAuthorizationWithDetails() {
        final Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(WorkerpoolAuthorization.builder().build());
        assertThat(isAuth).isNotEmpty()
                .contains(EMPTY_PARAMS_UNAUTHORIZED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskTypeNotTeeOnchainWithDetails() {
        final ChainDeal chainDeal = ChainDeal.builder()
                .poolOwner("0xc911f9345717ba7c8ec862ce002af3e058df84e4")
                .tag(BytesUtils.EMPTY_HEX_STRING_32)
                .build();
        final ChainTask chainTask = getChainTask(ACTIVE);
        final WorkerpoolAuthorization auth = getTeeWorkerpoolAuth();
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        final Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth);
        assertThat(isAuth).isNotEmpty()
                .contains(NO_MATCH_ONCHAIN_TYPE);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenGetTaskFailedWithDetails() {
        final WorkerpoolAuthorization auth = getTeeWorkerpoolAuth();
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.empty());

        final Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth);
        assertThat(isAuth).isNotEmpty()
                .contains(GET_CHAIN_TASK_FAILED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskNotActiveWithDetails() {
        final WorkerpoolAuthorization auth = getTeeWorkerpoolAuth();
        final ChainTask chainTask = getChainTask(UNSET);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));

        final Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth);
        assertThat(isAuth).isNotEmpty()
                .contains(TASK_NOT_ACTIVE);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenGetDealFailedWithDetails() {
        final ChainTask chainTask = getChainTask(ACTIVE);
        final WorkerpoolAuthorization auth = getTeeWorkerpoolAuth();

        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.empty());

        final Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth);
        assertThat(isAuth).isNotEmpty()
                .contains(GET_CHAIN_DEAL_FAILED);
    }

    @Test
    void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenPoolSignatureIsNotValidWithDetails() {
        final ChainDeal chainDeal = getChainDeal();
        final ChainTask chainTask = getChainTask(ACTIVE);
        final WorkerpoolAuthorization auth = getTeeWorkerpoolAuth();

        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        final Optional<AuthorizationError> isAuth = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(auth);
        assertThat(isAuth).isNotEmpty()
                .contains(INVALID_SIGNATURE);
    }
    // endregion

    // region challenges
    @Test
    void getChallengeForSetRequesterAppComputeSecret() {
        final String challenge = authorizationService.getChallengeForSetRequesterAppComputeSecret(
                "", "0", "");
        Assertions.assertEquals("0x31991eefc2731228bdd25dbc5a242722eda3869f9b06536dbd96a774e5228509",
                challenge);
    }

    @Test
    void getChallengeForSetWeb3Secret() {
        final String secretAddress = "0x123";
        final String secretValue = "ghijk";

        final String challenge = authorizationService.getChallengeForSetWeb3Secret(secretAddress, secretValue);
        Assertions.assertEquals("0x8d0b92aaf96f66f172d7615b81f257ebdece2278b7da6c60127cad45852eaaf6",
                challenge);
    }
    // endregion

    // region utils
    private ChainDeal getChainDeal() {
        return ChainDeal.builder()
                .poolOwner("0xc911f9345717ba7c8ec862ce002af3e058df84e4")
                .tag(TeeUtils.TEE_SCONE_ONLY_TAG)
                .build();
    }

    private static ChainTask getChainTask(ChainTaskStatus status) {
        return ChainTask.builder().dealid(DEAL_ID).status(status).build();
    }

    private WorkerpoolAuthorization getTeeWorkerpoolAuth() {
        final String hash = HashUtils.concatenateAndHash(WORKER_ADDRESS, CHAIN_TASK_ID, ENCLAVE_ADDRESS);
        final Signature signature = SignatureUtils.signMessageHashAndGetSignature(hash, POOL_PRIVATE);
        return WorkerpoolAuthorization.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_ADDRESS)
                .signature(signature)
                .build();
    }
    // endregion
}
