package com.iexec.sms.iexecsms.blockchain;

import com.iexec.common.contract.generated.App;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.ens.EnsResolutionException;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;

@Service
public class AppContractService {

    private final Web3j web3j;
    private final Credentials credentials;

    public AppContractService(Web3jService web3jService,
                              CredentialsService credentialsService) {
        web3j = web3jService.getWeb3j();
        credentials = credentialsService.getCredentials();
    }

    public App getAppContract(String appAddress) {
        ExceptionInInitializerError exceptionInInitializerError = new ExceptionInInitializerError("Failed to load App contract from address " + appAddress);

        if (appAddress != null && !appAddress.isEmpty()) {
            try {
                return App.load(
                        appAddress, web3j, credentials, new DefaultGasProvider());
            } catch (EnsResolutionException e) {
                throw exceptionInInitializerError;
            }
        } else {
            throw exceptionInInitializerError;
        }
    }
}
