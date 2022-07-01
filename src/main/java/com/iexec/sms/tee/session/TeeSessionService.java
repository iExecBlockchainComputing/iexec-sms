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

import com.iexec.common.task.TaskDescription;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.cas.CasClient;
import com.iexec.sms.tee.session.palaemon.PalaemonSessionRequest;
import com.iexec.sms.tee.session.palaemon.PalaemonSessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.iexec.sms.api.TeeSessionGenerationError.*;

@Slf4j
@Service
public class TeeSessionService {

    private final IexecHubService iexecHubService;
    private final CasClient casClient;
    private final PalaemonSessionService palaemonSessionService;
    private final boolean shouldDisplayDebugSession;

    public TeeSessionService(
            IexecHubService iexecService,
            PalaemonSessionService palaemonSessionService,
            CasClient casClient,
            @Value("${logging.tee.display-debug-session}")
            boolean shouldDisplayDebugSession) {
        this.iexecHubService = iexecService;
        this.palaemonSessionService = palaemonSessionService;
        this.casClient = casClient;
        this.shouldDisplayDebugSession = shouldDisplayDebugSession;
    }

    public String generateTeeSession(
            String taskId,
            String workerAddress,
            String teeChallenge) throws TeeSessionGenerationException {

        String sessionId = createSessionId(taskId);
        TaskDescription taskDescription = iexecHubService.getTaskDescription(taskId);
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(
                    GET_TASK_DESCRIPTION_FAILED,
                    String.format("Failed to get task description [taskId:%s]", taskId)
            );
        }
        PalaemonSessionRequest request = PalaemonSessionRequest.builder()
                .sessionId(sessionId)
                .taskDescription(taskDescription)
                .workerAddress(workerAddress)
                .enclaveChallenge(teeChallenge)
                .build();
        String sessionYmlAsString = palaemonSessionService.getSessionYml(request);
        if (sessionYmlAsString.isEmpty()) {
            throw new TeeSessionGenerationException(
                    GET_SESSION_YML_FAILED,
                    String.format("Failed to get session yml [taskId:%s, workerAddress:%s]", taskId, workerAddress));
        }
        log.info("Session yml is ready [taskId:{}]", taskId);
        if (shouldDisplayDebugSession){
            log.info("Session yml content [taskId:{}]\n{}", taskId, sessionYmlAsString);
        }
        // /!\ TODO clean expired tasks sessions
        boolean isSessionGenerated = casClient
                .generateSecureSession(sessionYmlAsString.getBytes())
                .getStatusCode()
                .is2xxSuccessful();
        if (!isSessionGenerated) {
            throw new TeeSessionGenerationException(
                    SECURE_SESSION_CAS_CALL_FAILED,
                    String.format("Failed to generate secure session [taskId:%s, workerAddress:%s]", taskId, workerAddress)
            );
        }
        return sessionId;
    }

    private String createSessionId(String taskId) {
        String randomString = RandomStringUtils.randomAlphanumeric(10);
        return String.format("%s0000%s", randomString, taskId);
    }
}
