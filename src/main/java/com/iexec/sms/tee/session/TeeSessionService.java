/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.TeeSessionGenerationResponse;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import static com.iexec.sms.api.TeeSessionGenerationError.GET_TASK_DESCRIPTION_FAILED;
import static com.iexec.sms.api.TeeSessionGenerationError.SECURE_SESSION_NO_TEE_PROVIDER;

@Service
public class TeeSessionService {

    private final IexecHubService iexecHubService;
    private final TeeSessionHandler teeSessionHandler;


    public TeeSessionService(
            IexecHubService iexecService,
            TeeSessionHandler teeSessionHandler) {
        this.iexecHubService = iexecService;
        this.teeSessionHandler = teeSessionHandler;
    }

    public TeeSessionGenerationResponse generateTeeSession(
            String taskId,
            String workerAddress,
            String teeChallenge) throws TeeSessionGenerationException {
        String sessionId = createSessionId(taskId);
        TaskDescription taskDescription = iexecHubService.getTaskDescription(taskId);
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(
                    GET_TASK_DESCRIPTION_FAILED,
                    String.format("Failed to get task description [taskId:%s]", taskId));
        }
        TeeSessionRequest request = TeeSessionRequest.builder()
                .sessionId(sessionId)
                .taskDescription(taskDescription)
                .workerAddress(workerAddress)
                .enclaveChallenge(teeChallenge)
                .build();

        final TeeFramework teeFramework = taskDescription.getTeeFramework();
        if (teeFramework == null) {
            throw new TeeSessionGenerationException(
                    SECURE_SESSION_NO_TEE_PROVIDER,
                    String.format("TEE framework can't be null [taskId:%s]", taskId));
        }

        // /!\ TODO clean expired tasks sessions
        String secretProvisioningUrl = teeSessionHandler.buildAndPostSession(request);
        return new TeeSessionGenerationResponse(sessionId, secretProvisioningUrl);
    }

    private String createSessionId(String taskId) {
        String randomString = RandomStringUtils.randomAlphanumeric(10);
        return String.format("%s0000%s", randomString, taskId);
    }

}
