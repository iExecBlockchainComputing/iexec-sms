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
import com.iexec.sms.api.config.TeeServicesConfiguration;
import com.iexec.sms.tee.EnableIfTeeProvider;
import com.iexec.sms.tee.EnableIfTeeProviderDefinition;
import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.gramine.sps.GramineEnclave;
import com.iexec.sms.tee.session.gramine.sps.GramineSession;
import com.iexec.sms.tee.session.gramine.sps.GramineSession.GramineSessionBuilder;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Conditional(EnableIfTeeProvider.class)
@EnableIfTeeProviderDefinition(providers = TeeEnclaveProvider.GRAMINE)
public class GramineSessionMakerService {

    private final SecretSessionBaseService secretSessionBaseService;
    private final TeeServicesConfiguration teeServicesConfig;

    public GramineSessionMakerService(SecretSessionBaseService secretSessionBaseService,
                                      TeeServicesConfiguration teeServicesConfig) {
        this.secretSessionBaseService = secretSessionBaseService;
        this.teeServicesConfig = teeServicesConfig;
    }

    /**
     * Collect tokens required for different compute stages (pre, in, post)
     * and build the JSON config of the TEE session.
     *
     * @param request session request details
     * @return session config
     */
    public GramineSession generateSession(TeeSessionRequest request) throws TeeSessionGenerationException {
        SecretSessionBase baseSession = secretSessionBaseService.getSecretsTokens(request);
        GramineSessionBuilder gramineSession = GramineSession.builder()
                .session(request.getSessionId());
        GramineEnclave gramineAppEnclave = toGramineEnclave(baseSession.getAppCompute());
        gramineAppEnclave.setCommand(request.getTaskDescription().getAppCommand());
        GramineEnclave graminePostEnclave = toGramineEnclave(baseSession.getPostCompute());
        graminePostEnclave.setCommand(teeServicesConfig.getPostComputeConfiguration().getEntrypoint());

        // TODO: Remove useless volumes when SPS is ready
        gramineAppEnclave.setVolumes(List.of());
        graminePostEnclave.setVolumes(List.of());

        return gramineSession.enclaves(List.of(
                // No pre-compute for now
                gramineAppEnclave,
                graminePostEnclave))
                .build();
    }

    private GramineEnclave toGramineEnclave(SecretEnclaveBase enclaveBase) {
        return GramineEnclave.builder()
                .name(enclaveBase.getName())
                .mrenclave(enclaveBase.getMrenclave())
                .environment(enclaveBase.getEnvironment())
                .build();
    }

}
