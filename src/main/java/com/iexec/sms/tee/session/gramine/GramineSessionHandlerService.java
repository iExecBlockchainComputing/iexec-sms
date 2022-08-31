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

package com.iexec.sms.tee.session.gramine;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.tee.EnableIfTeeProvider;
import com.iexec.sms.tee.EnableIfTeeProviderDefinition;
import com.iexec.sms.tee.session.TeeSessionLogConfiguration;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.gramine.sps.GramineSession;
import com.iexec.sms.tee.session.gramine.sps.SpsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Conditional(EnableIfTeeProvider.class)
@EnableIfTeeProviderDefinition(providers = TeeEnclaveProvider.GRAMINE)
public class GramineSessionHandlerService implements TeeSessionHandler {
    private GramineSessionMakerService sessionService;
    private SpsConfiguration spsConfiguration;
    private TeeSessionLogConfiguration teeSessionLogConfiguration;

    public GramineSessionHandlerService(GramineSessionMakerService sessionService,
            SpsConfiguration spsConfiguration,
            TeeSessionLogConfiguration teeSessionLogConfiguration) {
        this.sessionService = sessionService;
        this.spsConfiguration = spsConfiguration;
        this.teeSessionLogConfiguration = teeSessionLogConfiguration;
    }

    /**
     * Build and post secret session on secret provisioning service.
     * 
     * @param request tee session generation request
     * @return String secret provisioning service url
     * @throws TeeSessionGenerationException
     */
    @Override
    public String buildAndPostSession(TeeSessionRequest request)
            throws TeeSessionGenerationException {
        GramineSession session = sessionService.generateSession(request);
        if (session != null
                && teeSessionLogConfiguration.isDisplayDebugSessionEnabled()) {
            log.info("Session content [taskId:{}]\n{}",
                    request.getTaskDescription().getChainTaskId(), session.toString());
        }

        try {
            spsConfiguration.getInstance().postSession(session);
            return spsConfiguration.getEnclaveHost();
        } catch (Exception e) {
            throw new TeeSessionGenerationException(
                    TeeSessionGenerationError.SECURE_SESSION_STORAGE_CALL_FAILED,
                    "Failed to post session: " + e.getMessage());
        }
    }
}
