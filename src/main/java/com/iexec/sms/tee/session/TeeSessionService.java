package com.iexec.sms.tee.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TeeSessionService {

    private final TeeSessionClient teeSessionClient;
    private final TeeSessionHelper teeSessionHelper;
    private final boolean shouldDisplayDebugSession;

    public TeeSessionService(
            TeeSessionHelper teeSessionHelper,
            TeeSessionClient teeSessionClient,
            @Value("${logging.tee.display-debug-session}") boolean shouldDisplayDebugSession) {
        this.teeSessionHelper = teeSessionHelper;
        this.teeSessionClient = teeSessionClient;
        this.shouldDisplayDebugSession = shouldDisplayDebugSession;
    }

    public String generateTeeSession(String taskId, String workerAddress, String teeChallenge) {
        String sessionId = String.format("%s0000%s", RandomStringUtils.randomAlphanumeric(10), taskId);
        String sessionYmlAsString = teeSessionHelper.getPalaemonSessionYmlAsString(sessionId, taskId, workerAddress, teeChallenge);
        if (sessionYmlAsString.isEmpty()) {
            log.error("Failed to get session yml [taskId:{}, workerAddress:{}]", taskId, workerAddress);
            return "";
        }

        if (shouldDisplayDebugSession){
            log.info("Session yml is ready [taskId:{}, sessionYml:\n{}]", taskId, sessionYmlAsString);
        } else {
            log.info("Session yml is ready [taskId:{}, shouldDisplayDebugSession:{}]", taskId, false);
        }

        boolean isSessionGenerated = teeSessionClient.generateSecureSession(sessionYmlAsString.getBytes())
                .getStatusCode().is2xxSuccessful();
        return isSessionGenerated ? sessionId : "";
    }
}
