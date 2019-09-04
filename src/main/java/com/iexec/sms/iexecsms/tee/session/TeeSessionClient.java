package com.iexec.sms.iexecsms.tee.session;

import com.iexec.sms.iexecsms.ssl.SslRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TeeSessionClient {

    private TeeSessionConfiguration teeSessionConfiguration;
    private SslRestClient sslRestClient;

    public TeeSessionClient(
            TeeSessionConfiguration teeSessionConfiguration,
            SslRestClient sslRestClient) {
        this.teeSessionConfiguration = teeSessionConfiguration;
        this.sslRestClient = sslRestClient;
    }

    boolean generateSecureSession(byte[] palaemonFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        headers.set("Expect", "100-continue");
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(palaemonFile, headers);
        ResponseEntity<String> response = null;
        try {
            response = sslRestClient.getRestTemplate().exchange(teeSessionConfiguration.getCasUrl() + "/session",
                    HttpMethod.POST, httpEntity, String.class);
        } catch (Exception e) {
            log.error("Failed to generateSecureSession");
        }

        return response != null && response.getStatusCode().is2xxSuccessful();
    }

}
