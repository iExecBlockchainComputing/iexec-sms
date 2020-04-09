package com.iexec.sms.blockchain;


import com.iexec.common.chain.IexecHubAbstractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class IexecHubService extends IexecHubAbstractService {

    private final CredentialsService credentialsService;
    private Web3jService web3jService;

    @Autowired
    public IexecHubService(CredentialsService credentialsService,
                           Web3jService web3jService,
                           BlockchainConfig blockchainConfig) {
        super(credentialsService.getCredentials(), web3jService, blockchainConfig.getHubAddress());
        this.credentialsService = credentialsService;
        this.web3jService = web3jService;
    }

}
