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

import com.iexec.sms.tee.session.EnclaveEnvironment;
import com.iexec.sms.tee.session.EnclaveEnvironments;
import com.iexec.sms.tee.session.TeeSecretsService;
import com.iexec.sms.tee.session.generic.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.gramine.sps.SpsSession;
import com.iexec.sms.tee.session.gramine.sps.SpsSession.SpsSessionBuilder;
import com.iexec.sms.tee.session.gramine.sps.SpsSessionEnclave;
import com.iexec.sms.tee.session.gramine.sps.SpsSessionEnclave.SpsSessionEnclaveBuilder;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class GramineSessionMakerService {

    private final TeeSecretsService teeSecretsService;
    private TeeWorkflowConfiguration teeWorkflowConfiguration;

    public GramineSessionMakerService(TeeSecretsService teeSecretsService,
            TeeWorkflowConfiguration teeWorkflowConfiguration) {
        this.teeSecretsService = teeSecretsService;
        this.teeWorkflowConfiguration = teeWorkflowConfiguration;
    }

    /**
     * Collect tokens required for different compute stages (pre, in, post)
     * and build the JSON config of the TEE session.
     *
     * @param request session request details
     * @return session config
     */
    public SpsSession generateSession(TeeSecretsSessionRequest request) throws TeeSessionGenerationException {
        EnclaveEnvironments enclaveEnvironments = teeSecretsService.getSecretsTokens(request);
        SpsSessionBuilder sessionBuilder = SpsSession.builder();
        sessionBuilder.session(request.getSessionId());
        SpsSessionEnclave appSessionEnclave = toSpsSessionEnclave(enclaveEnvironments.getAppCompute());
        appSessionEnclave.setCommand(request.getTaskDescription().getAppCommand());
        SpsSessionEnclave postSessionEnclave = toSpsSessionEnclave(enclaveEnvironments.getPostCompute());
        postSessionEnclave.setMrenclave(teeWorkflowConfiguration.getPostComputeFingerprint());
        postSessionEnclave.setCommand(teeWorkflowConfiguration.getPostComputeEntrypoint());

        //TODO: Remove useless volumes when SPS is ready
        appSessionEnclave.setVolumes(List.of());
        postSessionEnclave.setVolumes(List.of());

        sessionBuilder.enclaves(Arrays.asList(
                // No pre-compute for now
                appSessionEnclave,
                postSessionEnclave));
        return sessionBuilder.build();
    }

    private SpsSessionEnclave toSpsSessionEnclave(EnclaveEnvironment enclaveEnvironment) {
        SpsSessionEnclaveBuilder enclavebuilder = SpsSessionEnclave.builder();
        enclavebuilder.name(enclaveEnvironment.getName());
        enclavebuilder.mrenclave(enclaveEnvironment.getMrenclave());
        enclavebuilder.environment(enclaveEnvironment.getEnvironment());
        return enclavebuilder.build();
    }

}
