/*
 * Copyright 2022-2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.gramine.sps.GramineEnclave;
import com.iexec.sms.tee.session.gramine.sps.GramineSession;
import com.iexec.sms.tee.session.gramine.sps.GramineSession.GramineSessionBuilder;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnTeeFramework(frameworks = TeeFramework.GRAMINE)
public class GramineSessionMakerService {

    private final SecretSessionBaseService secretSessionBaseService;

    public GramineSessionMakerService(SecretSessionBaseService secretSessionBaseService) {
        this.secretSessionBaseService = secretSessionBaseService;
    }

    /**
     * Collect tokens required for different compute stages (pre, in, post)
     * and build the JSON config of the TEE session.
     *
     * @param request session request details
     * @return session config
     */
    @NonNull
    public GramineSession generateSession(TeeSessionRequest request) throws TeeSessionGenerationException {
        SecretSessionBase baseSession = secretSessionBaseService.getSecretsTokens(request);
        GramineSessionBuilder gramineSession = GramineSession.builder()
                .session(request.getSessionId());
        GramineEnclave gramineAppEnclave = toGramineEnclave(baseSession.getAppCompute());
        GramineEnclave graminePostEnclave = toGramineEnclave(baseSession.getPostCompute());

        // TODO: Validate command-line arguments from the host
        // (https://github.com/gramineproject/gsc/issues/13)
        gramineAppEnclave.setCommand("");
        graminePostEnclave.setCommand("");
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
