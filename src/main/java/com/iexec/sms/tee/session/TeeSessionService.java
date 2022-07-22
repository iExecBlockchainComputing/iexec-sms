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
import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.gramine.GramineSessionHandlerService;
import com.iexec.sms.tee.session.scone.SconeSessionHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.iexec.sms.api.TeeSessionGenerationError.*;

@Slf4j
@Service
public class TeeSessionService {

    private final IexecHubService iexecHubService;

    private final Map<TeeEnclaveProvider, TeeSessionHandler> teeSessionConfigurations;

    public TeeSessionService(
            IexecHubService iexecService,
            SconeSessionHandlerService sconeService,
            GramineSessionHandlerService gramineService) {
        this.iexecHubService = iexecService;
        this.teeSessionConfigurations = Map.of(
                TeeEnclaveProvider.SCONE, sconeService,
                TeeEnclaveProvider.GRAMINE, gramineService);
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
                    String.format("Failed to get task description [taskId:%s]", taskId));
        }
        TeeSecretsSessionRequest request = TeeSecretsSessionRequest.builder()
                .sessionId(sessionId)
                .taskDescription(taskDescription)
                .workerAddress(workerAddress)
                .enclaveChallenge(teeChallenge)
                .build();

        final TeeEnclaveProvider teeEnclaveProvider = taskDescription.getTeeEnclaveProvider();
        if (teeEnclaveProvider == null) {
            throw new TeeSessionGenerationException(
                    SECURE_SESSION_NO_TEE_PROVIDER,
                    String.format("TEE provider can't be null [taskId:%s]", taskId));
        }

        final TeeSessionHandler teeSessionHandler = teeSessionConfigurations.get(teeEnclaveProvider);
        if (teeSessionHandler == null) {
            throw new TeeSessionGenerationException(
                    SECURE_SESSION_UNKNOWN_TEE_PROVIDER,
                    String.format("Unknown TEE provider [taskId:%s, teeProvider:%s]", taskId, teeEnclaveProvider));
        }

        // /!\ TODO clean expired tasks sessions
        teeSessionHandler.buildAndPostSession(request);
        return sessionId;
    }

    private String createSessionId(String taskId) {
        String randomString = RandomStringUtils.randomAlphanumeric(10);
        return String.format("%s0000%s", randomString, taskId);
    }

}
