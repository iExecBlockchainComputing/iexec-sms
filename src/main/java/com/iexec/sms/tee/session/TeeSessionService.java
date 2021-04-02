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

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.palaemon.PalaemonSessionRequest;
import com.iexec.sms.tee.session.palaemon.PalaemonSessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeeSessionService {

    private final IexecHubService iexecHubService;
    private final TeeSessionClient teeSessionClient;
    private final PalaemonSessionService palaemonSessionService;
    private final boolean shouldDisplayDebugSession;

    public TeeSessionService(
            IexecHubService iexecService,
            PalaemonSessionService palaemonSessionService,
            TeeSessionClient teeSessionClient,
            @Value("${logging.tee.display-debug-session}") boolean shouldDisplayDebugSession) {
        this.iexecHubService = iexecService;
        this.palaemonSessionService = palaemonSessionService;
        this.teeSessionClient = teeSessionClient;
        this.shouldDisplayDebugSession = shouldDisplayDebugSession;
    }

    public String generateTeeSession(
            String taskId,
            String workerAddress,
            String teeChallenge) throws Exception {

        Optional<ChainTask> chainTask = iexecHubService.getChainTask(taskId);
        if (chainTask.isEmpty()) {
            log.error("Failed to get chain task [taskId:{}, workerAddress:{}]",
                    taskId, workerAddress);
            return "";
        }
        Optional<ChainDeal> chainDeal = iexecHubService.getChainDeal(chainTask.get().getDealid());
        if (chainDeal.isEmpty()) {
            throw new Exception("Failed to get chain deal - taskId: " + taskId);
        }
        String sessionId = createSessionId(taskId);
        PalaemonSessionRequest request = PalaemonSessionRequest.builder()
                .sessionId(sessionId)
                .chainTaskId(taskId)
                .workerAddress(workerAddress)
                .enclaveChallenge(teeChallenge)
                .chainDeal(chainDeal.get())
                .build();
        String sessionYmlAsString = palaemonSessionService.getSessionYml(request);
        if (sessionYmlAsString.isEmpty()) {
            throw new Exception("Failed to get session yml [taskId:" + taskId + "," +
                    " workerAddress:" + workerAddress);
        }
        log.info("Session yml is ready [taskId:{}]", taskId);
        if (shouldDisplayDebugSession){
            log.info("Session yml content [taskId:{}]\n{}", taskId, sessionYmlAsString);
        }
        boolean isSessionGenerated = teeSessionClient
                .generateSecureSession(sessionYmlAsString.getBytes())
                .getStatusCode()
                .is2xxSuccessful();
        return isSessionGenerated ? sessionId : "";
    }

    private String createSessionId(String taskId) {
        String randomString = RandomStringUtils.randomAlphanumeric(10);
        return String.format("%s0000%s", randomString, taskId);
    }
}
