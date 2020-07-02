package com.iexec.sms.tee.session;

import com.iexec.sms.ssl.TwoWaySslClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TeeSessionClient {

    private final TeeCasConfiguration teeCasConfiguration;
    private final TwoWaySslClient twoWaySslClient;

    public TeeSessionClient(TeeCasConfiguration teeCasConfiguration,
                            TwoWaySslClient twoWaySslClient) {
        this.teeCasConfiguration = teeCasConfiguration;
        this.twoWaySslClient = twoWaySslClient;
    }

    /*
     * POST /session of CAS requires 2-way SSL authentication
     * */
    public ResponseEntity<String> generateSecureSession(byte[] palaemonFile) {
        try {
            return twoWaySslClient.getRestTemplate().postForEntity(teeCasConfiguration.getCasUrl() + "/session",
                    new HttpEntity<>(palaemonFile), String.class);
        } catch (Exception e) {
            log.error("Failed to generateSecureSession [exceptionMessage:{}]", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

}