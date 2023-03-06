package com.iexec.sms;

import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.blockchain.Web3jService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.iexec.sms.MockChainConfiguration.MOCK_CHAIN_PROFILE;

@Configuration
@Profile(MOCK_CHAIN_PROFILE)
public class MockChainConfiguration {
    public static final String MOCK_CHAIN_PROFILE = "mock-chain";

    @MockBean
    protected IexecHubService iexecHubService;
    @MockBean
    protected Web3jService web3jService;
}
