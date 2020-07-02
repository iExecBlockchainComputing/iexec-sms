package com.iexec.sms.tee.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TeeSessionService {

    private final TeeSessionClient teeSessionClient;
    private final TeeSessionHelper teeSessionHelper;

    public TeeSessionService(
            TeeSessionHelper teeSessionHelper,
            TeeSessionClient teeSessionClient) {
        this.teeSessionHelper = teeSessionHelper;
        this.teeSessionClient = teeSessionClient;
    }

    public String generateTeeSession(String taskId, String workerAddress, String teeChallenge) {
        String sessionId = String.format("%s0000%s", RandomStringUtils.randomAlphanumeric(10), taskId);
        String sessionYmlAsString = teeSessionHelper.getPalaemonSessionYmlAsString(sessionId, taskId, workerAddress, teeChallenge);
        if (sessionYmlAsString.isEmpty()) {
            log.error("Failed to get session yml [taskId:{}, workerAddress:{}]", taskId, workerAddress);
            return "";
        }

        log.info("## Palaemon session YML ##"); //dev logs, lets keep them for now
        log.info(sessionYmlAsString);
        log.info("#####################");

        boolean isSessionGenerated = teeSessionClient.generateSecureSession(sessionYmlAsString.getBytes())
                .getStatusCode().is2xxSuccessful();
        return isSessionGenerated ? sessionId : "";
    }
}
