package com.iexec.sms.iexecsms.cas;

import feign.Client;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Service
@Slf4j
public class CasService {

    private CasClient casClient;
    private CasConfigurationService casConfigurationService;

    public CasService(CasClient casClient,
                      CasConfigurationService casConfigurationService) {
        this.casClient = casClient;
        this.casConfigurationService = casConfigurationService;
    }

    /*
     * RestTemplate used for generating session
     * */
    public boolean generateSecureSessionWithRestTemplate(byte[] palaemonFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        headers.set("Expect", "100-continue");
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(palaemonFile, headers);
        ResponseEntity<String> response = null;
        try {
            response = casConfigurationService.getRestTemplate().exchange(casConfigurationService.getCasUrl() + "/session",
                    HttpMethod.POST, httpEntity, String.class);
            System.out.println("response status: " + response.getStatusCode());
            System.out.println("response body: " + response.getBody());
        } catch (Exception e) {
            log.error("Why?");
        }

        return response != null && response.getStatusCode().is2xxSuccessful();
    }

    /*
     * Feign not used for generating session
     * */
    public boolean generateSecureSessionWithFeign(byte[] palaemonFile) {
        ResponseEntity generateSessionResponse = casClient.generateSecureSessionWithPalaemonFile(palaemonFile);
        return generateSessionResponse.getStatusCode().is2xxSuccessful();
    }
}
