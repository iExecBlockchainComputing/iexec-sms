/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.scone;

import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.tee.session.*;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.scone.cas.CasClient;
import com.iexec.sms.tee.session.scone.palaemon.PalaemonSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SconeSessionHandlerService implements TeeSessionHandler {
    private PalaemonSessionService sessionService;
    private CasClient client;
    private TeeSessionLogConfiguration teeSessionLogConfiguration;

    public SconeSessionHandlerService(PalaemonSessionService sessionService,
            CasClient client,
            TeeSessionLogConfiguration teeSessionLogConfiguration) {
        this.sessionService = sessionService;
        this.client = client;
        this.teeSessionLogConfiguration = teeSessionLogConfiguration;
    }

    @Override
    public void buildAndPostSession(TeeSecretsSessionRequest request)
            throws TeeSessionGenerationException {
        String session = sessionService.generateSession(request);
        if (session != null
                && teeSessionLogConfiguration.isDisplayDebugSessionEnabled()) {
            log.info("Session content [taskId:{}]\n{}",
                    request.getTaskDescription().getChainTaskId(), session);
        }
        ResponseEntity<String> postSession = client.postSession(session);
        int httpCode = postSession != null ? postSession.getStatusCodeValue() : null;
        if (httpCode != 200) {
            throw new TeeSessionGenerationException(
                    TeeSessionGenerationError.SECURE_SESSION_STORAGE_CALL_FAILED,
                    "Failed to post session: " + httpCode);
        }
    }

}