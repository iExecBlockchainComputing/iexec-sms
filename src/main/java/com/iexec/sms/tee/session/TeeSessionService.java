package com.iexec.sms.tee.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import feign.FeignException;

@Slf4j
@Service
public class TeeSessionService {

    private TeeSessionClient teeSessionClient;
    private TeeSessionHelper teeSessionHelper;

    public TeeSessionService(
            TeeSessionHelper teeSessionHelper,
            TeeSessionClient teeSessionClient) {
        this.teeSessionHelper = teeSessionHelper;
        this.teeSessionClient = teeSessionClient;
    }

    public String generateTeeSession(String taskId, String workerAddress, String teeChallenge) throws FeignException {
        String sessionId = String.format("%s0000%s", RandomStringUtils.randomAlphanumeric(10), taskId);
        String sessionYmlAsString = teeSessionHelper.getPalaemonSessionYmlAsString(sessionId, taskId, workerAddress, teeChallenge);
        if (sessionYmlAsString.isEmpty()) {
            log.error("Failed to get session yml [taskId:{}, workerAddress:{}]", taskId, workerAddress);
            return "";
        }

        System.out.println("## Palaemon session YML ##"); //dev logs, lets keep them for now
        System.out.println(sessionYmlAsString);
        System.out.println("#####################");

        ResponseEntity<String> response = teeSessionClient.generateSecureSession(sessionYmlAsString.getBytes());
        return (response != null && response.getStatusCode().is2xxSuccessful()) ? sessionId : "";
    }
}
