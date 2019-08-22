package com.iexec.sms.iexecsms.cas;

import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class CasService {

    private CasClient casClient;

    public CasService(CasClient casClient) {
        this.casClient = casClient;
    }

    @Retryable(value = FeignException.class)
    public ResponseEntity generateSecureSessionWithPalaemonFile(String palaemonFile) {
        return casClient.generateSecureSessionWithPalaemonFile(palaemonFile);
    }
}
