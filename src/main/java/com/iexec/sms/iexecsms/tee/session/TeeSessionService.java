package com.iexec.sms.iexecsms.tee.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Service;

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

    public String generateTeeSession(String taskId, String workerAddress, String teeChallenge) throws Exception {
        String sessionId = String.format("%s0000%s", RandomStringUtils.randomAlphanumeric(10), taskId);

        String sessionYmlAsString = teeSessionHelper.getPalaemonSessionYmlAsString(sessionId, taskId, workerAddress, teeChallenge);
        if (sessionYmlAsString.isEmpty()) {
            log.error("Failed to generateTeeSession (empty sessionYml)[taskId:{}]", taskId);
            return "";
        }

        System.out.println("## Palaemon session YML ##"); //dev logs, lets keep them for now
        System.out.println(sessionYmlAsString);
        System.out.println("#####################");

        boolean isSessionCreated = teeSessionClient.generateSecureSession(sessionYmlAsString.getBytes());

        if (!isSessionCreated) {
            log.error("Failed to generateTeeSession (cant generateSecureSession)[taskId:{}]", taskId);
            return "";
        }

        return sessionId;
    }
}
