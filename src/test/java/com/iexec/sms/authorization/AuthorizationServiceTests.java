package com.iexec.sms.authorization;

import static com.iexec.common.chain.ChainTaskStatus.ACTIVE;
import static com.iexec.common.chain.ChainTaskStatus.UNSET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.ContributionAuthorization;
import com.iexec.common.security.Signature;
import com.iexec.common.utils.TestUtils;
import com.iexec.sms.blockchain.IexecHubService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuthorizationServiceTests {

    @Mock
    IexecHubService iexecHubService;

    @InjectMocks
    private AuthorizationService authorizationService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBeAuthorizedOnExecutionOfTeeTask() {
        ChainDeal chainDeal = TestUtils.getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        ContributionAuthorization auth = TestUtils.getTeeContributionAuth();
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        boolean isAuth = authorizationService.isAuthorizedOnExecution(auth, true);
        assertThat(isAuth).isTrue();
    }

    @Test
    public void shouldNotBeAuthorizedOnExecutionOfTeeTaskWithNullAuthorization() {
        boolean isAuth = authorizationService.isAuthorizedOnExecution(null, true);
        assertThat(isAuth).isFalse();
    }

    @Test
    public void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskTypeNotMatchedOnchain() {
        ContributionAuthorization auth = TestUtils.getTeeContributionAuth();
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);

        boolean isAuth = authorizationService.isAuthorizedOnExecution(auth, false);
        assertThat(isAuth).isFalse();
    }

    @Test
    public void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenTaskNotActive() {
        ContributionAuthorization auth = TestUtils.getTeeContributionAuth();
        ChainTask chainTask = TestUtils.getChainTask(UNSET);
        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));

        boolean isAuth = authorizationService.isAuthorizedOnExecution(auth, true);
        assertThat(isAuth).isFalse();
    }

    @Test
    public void shouldNotBeAuthorizedOnExecutionOfTeeTaskWhenPoolSignatureIsNotValid() {
        ChainDeal chainDeal = TestUtils.getChainDeal();
        ChainTask chainTask = TestUtils.getChainTask(ACTIVE);
        ContributionAuthorization auth = TestUtils.getTeeContributionAuth();
        auth.setSignature(new Signature(TestUtils.POOL_WRONG_SIGNATURE));

        when(iexecHubService.isTeeTask(auth.getChainTaskId())).thenReturn(true);
        when(iexecHubService.getChainTask(auth.getChainTaskId())).thenReturn(Optional.of(chainTask));
        when(iexecHubService.getChainDeal(chainTask.getDealid())).thenReturn(Optional.of(chainDeal));

        boolean isAuth = authorizationService.isAuthorizedOnExecution(auth, true);
        assertThat(isAuth).isFalse();
    }
}