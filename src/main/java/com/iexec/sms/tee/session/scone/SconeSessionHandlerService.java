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

import com.iexec.common.tee.TeeFramework;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import com.iexec.sms.tee.scone.SconeSession;
import com.iexec.sms.tee.session.TeeSessionLogConfiguration;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.scone.cas.CasClient;
import com.iexec.sms.tee.session.scone.cas.CasConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
public class SconeSessionHandlerService implements TeeSessionHandler {
    private final SconeSessionMakerService sessionService;
    private final CasClient apiClient;
    private final TeeSessionLogConfiguration teeSessionLogConfiguration;
    private final CasConfiguration casConfiguration;

    public SconeSessionHandlerService(SconeSessionMakerService sessionService,
                                      CasClient apiClient,
                                      TeeSessionLogConfiguration teeSessionLogConfiguration,
                                      CasConfiguration casConfiguration) {
        this.sessionService = sessionService;
        this.apiClient = apiClient;
        this.teeSessionLogConfiguration = teeSessionLogConfiguration;
        this.casConfiguration = casConfiguration;
    }

    /**
     * Build and post secret session on secret provisioning service.
     * 
     * @param request tee session generation request
     * @return String secret provisioning service url
     * @throws TeeSessionGenerationException if call to CAS failed
     */
    @Override
    public String buildAndPostSession(TeeSessionRequest request)
            throws TeeSessionGenerationException {
        SconeSession session = sessionService.generateSession(request);
        if (teeSessionLogConfiguration.isDisplayDebugSessionEnabled()) {
            log.info("Session content [taskId:{}]\n{}",
                    request.getTaskDescription().getChainTaskId(), session);
        }
        final String sessionAsYaml = sessionService.getSessionAsYaml(session);
        ResponseEntity<String> postSession = apiClient.postSession(sessionAsYaml);

        if (postSession == null) {
            throw new TeeSessionGenerationException(
                    TeeSessionGenerationError.SECURE_SESSION_STORAGE_CALL_FAILED,
                    "Failed to post session, no return from CAS.");
        }

        int httpCode = postSession.getStatusCodeValue();
        if (httpCode != 201) {
            throw new TeeSessionGenerationException(
                    TeeSessionGenerationError.SECURE_SESSION_STORAGE_CALL_FAILED,
                    "Failed to post session: " + httpCode);
        }
        return casConfiguration.getEnclaveHost();
    }

}
