package com.iexec.sms.iexecsms.cas;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CasService {

    private CasConfigurationService casConfigurationService;

    public CasService(CasConfigurationService casConfigurationService) {
        this.casConfigurationService = casConfigurationService;
    }

    public boolean generateSecureSession(byte[] palaemonFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        headers.set("Expect", "100-continue");
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(palaemonFile, headers);
        ResponseEntity<String> response = null;
        try {
            response = casConfigurationService.getRestTemplate().exchange(casConfigurationService.getCasUrl() + "/session",
                    HttpMethod.POST, httpEntity, String.class);
        } catch (Exception e) {
            log.error("Failed to generateSecureSession");
        }

        return response != null && response.getStatusCode().is2xxSuccessful();
    }

}
