package com.iexec.sms.iexecsms.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@Slf4j
@Service
public class CredentialsService {

    private Credentials credentials;

    public CredentialsService() {
        try {
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            credentials = Credentials.create(ecKeyPair);
            log.info("Load wallet beneficiaryCredentials (new) [address:{}] ", credentials.getAddress());
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            log.error("Credentials cannot be loaded [exception:{}] ", e);
        }
    }

    public Credentials getCredentials() {
        return credentials;
    }

}