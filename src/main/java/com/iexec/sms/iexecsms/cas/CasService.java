package com.iexec.sms.iexecsms.cas;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CasService {

    private CasClient casClient;

    public CasService(CasClient casClient) {
        this.casClient = casClient;
    }

    //@Retryable(value = FeignException.class)
    public boolean generateSecureSessionWithPalaemonFile(byte[] palaemonFile) {
        ResponseEntity generateSessionResponse = casClient.generateSecureSessionWithPalaemonFile(palaemonFile);
        return generateSessionResponse.getStatusCode().is2xxSuccessful();
    }
}
