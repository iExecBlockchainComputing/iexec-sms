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

import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.gramine.sps.SpsEnclave;
import com.iexec.sms.tee.session.gramine.sps.SpsEnclave.SpsEnclaveBuilder;
import com.iexec.sms.tee.session.gramine.sps.SpsSession;
import com.iexec.sms.tee.session.gramine.sps.SpsSession.SpsSessionBuilder;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class GramineSessionMakerService {

    private final SecretSessionBaseService secretSessionBaseService;
    private TeeWorkflowConfiguration teeWorkflowConfiguration;

    public GramineSessionMakerService(SecretSessionBaseService secretSessionBaseService,
            TeeWorkflowConfiguration teeWorkflowConfiguration) {
        this.secretSessionBaseService = secretSessionBaseService;
        this.teeWorkflowConfiguration = teeWorkflowConfiguration;
    }

    /**
     * Collect tokens required for different compute stages (pre, in, post)
     * and build the JSON config of the TEE session.
     *
     * @param request session request details
     * @return session config
     */
    public SpsSession generateSession(TeeSessionRequest request) throws TeeSessionGenerationException {
        SecretSessionBase baseSession = secretSessionBaseService.getSecretsTokens(request);
        SpsSessionBuilder spsSession = SpsSession.builder();
        spsSession.session(request.getSessionId());
        SpsEnclave spsAppEnclave = toSpsSessionEnclave(baseSession.getAppCompute());
        spsAppEnclave.setCommand(request.getTaskDescription().getAppCommand());
        SpsEnclave spsPostEnclave = toSpsSessionEnclave(baseSession.getPostCompute());
        spsPostEnclave.setMrenclave(teeWorkflowConfiguration.getPostComputeFingerprint());
        spsPostEnclave.setCommand(teeWorkflowConfiguration.getPostComputeEntrypoint());

        // TODO: Remove useless volumes when SPS is ready
        spsAppEnclave.setVolumes(List.of());
        spsPostEnclave.setVolumes(List.of());

        spsSession.enclaves(Arrays.asList(
                // No pre-compute for now
                spsAppEnclave,
                spsPostEnclave));
        return spsSession.build();
    }

    private SpsEnclave toSpsSessionEnclave(SecretEnclaveBase enclaveBase) {
        SpsEnclaveBuilder spsEnclave = SpsEnclave.builder();
        spsEnclave.name(enclaveBase.getName());
        spsEnclave.mrenclave(enclaveBase.getMrenclave());
        spsEnclave.environment(enclaveBase.getEnvironment());
        return spsEnclave.build();
    }

}
