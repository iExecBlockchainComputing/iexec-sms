package com.iexec.sms.iexecsms.cas;

import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;

public class CasService {

    private CasClient casClient;

    public CasService(CasClient casClient) {
        this.casClient = casClient;
    }

    // TODO: rename this method with a more relevant name
    @Retryable(value = FeignException.class)
    public ResponseEntity postStuffWithPalaemon(String configFile) {
        return casClient.postStuffWithPalaemon(configFile);
    }
}
