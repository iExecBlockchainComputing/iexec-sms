/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.tdx;

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import com.iexec.sms.tee.session.base.SecretEnclaveBase;
import com.iexec.sms.tee.session.base.SecretSessionBase;
import com.iexec.sms.tee.session.base.SecretSessionBaseService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.tdx.storage.TdxSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnTeeFramework(frameworks = TeeFramework.TDX)
public class TdxSessionMakerService {
    public static final String TDX_SESSION_VERSION = "0.1.0";
    private final SecretSessionBaseService secretSessionBaseService;

    public TdxSessionMakerService(final SecretSessionBaseService secretSessionBaseService) {
        this.secretSessionBaseService = secretSessionBaseService;
    }

    public TdxSession generateSession(final TeeSessionRequest request) throws TeeSessionGenerationException {
        final SecretSessionBase baseSession = secretSessionBaseService.getSecretsTokens(request);
        final List<TdxSession.Service> tdxEnclaves = new ArrayList<>();
        if (baseSession.getPreCompute() != null) {
            tdxEnclaves.add(toTdxEnclave(baseSession.getPreCompute(), request.getTeeServicesProperties().getPreComputeProperties()));
        }
        tdxEnclaves.add(toTdxEnclave(baseSession.getAppCompute(), request.getTaskDescription().getAppUri(), request.getTaskDescription().getAppChecksum()));
        tdxEnclaves.add(toTdxEnclave(baseSession.getPostCompute(), request.getTeeServicesProperties().getPostComputeProperties()));
        return new TdxSession(request.getSessionId(), TDX_SESSION_VERSION, List.copyOf(tdxEnclaves));
    }

    private TdxSession.Service toTdxEnclave(final SecretEnclaveBase enclaveBase, final TeeAppProperties teeAppProperties) {
        return toTdxEnclave(enclaveBase, teeAppProperties.getImage(), teeAppProperties.getFingerprint());
    }

    private TdxSession.Service toTdxEnclave(final SecretEnclaveBase enclaveBase, final String image_name, final String fingerprint) {
        return new TdxSession.Service(
                enclaveBase.getName(), image_name, fingerprint, enclaveBase.getEnvironment()
        );
    }
}
