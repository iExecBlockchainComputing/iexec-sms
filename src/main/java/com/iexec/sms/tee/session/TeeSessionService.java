/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
