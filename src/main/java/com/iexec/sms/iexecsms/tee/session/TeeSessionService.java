package com.iexec.sms.iexecsms.tee.session;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Service;

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

        String configFile = teeSessionHelper.getPalaemonConfigurationFile(sessionId, taskId, workerAddress, teeChallenge);

        System.out.println("## Palaemon config ##"); //dev logs, lets keep them for now
        System.out.println(configFile);
        System.out.println("#####################");

        boolean isSessionCreated = teeSessionClient.generateSecureSession(configFile.getBytes());

        if (isSessionCreated) {
            return sessionId;
        }
        return "";
    }
}
