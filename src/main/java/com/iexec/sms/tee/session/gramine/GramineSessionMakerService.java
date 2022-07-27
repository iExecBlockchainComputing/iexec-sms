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

import com.iexec.common.utils.FileHelper;
import com.iexec.sms.tee.session.EnclaveEnvironment;
import com.iexec.sms.tee.session.EnclaveEnvironments;
import com.iexec.sms.tee.session.TeeSecretsService;
import com.iexec.sms.tee.session.generic.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.gramine.sps.SpsSession;
import com.iexec.sms.tee.session.gramine.sps.SpsSession.SpsSessionBuilder;
import com.iexec.sms.tee.session.gramine.sps.SpsSessionEnclave;
import com.iexec.sms.tee.session.gramine.sps.SpsSessionEnclave.SpsSessionEnclaveBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

@Service
public class GramineSessionMakerService {

    private final TeeSecretsService teeSecretsService;

    @Value("${gramine.sps.templateFile}")
    private String gramineTemplateFilePath;

    public GramineSessionMakerService(TeeSecretsService teeSecretsService) {
        this.teeSecretsService = teeSecretsService;
    }

    @PostConstruct
    void postConstruct() throws FileNotFoundException {
        if (StringUtils.isEmpty(gramineTemplateFilePath)) {
            throw new IllegalArgumentException("Missing gramine template filepath");
        }
        if (!FileHelper.exists(gramineTemplateFilePath)) {
            throw new FileNotFoundException("Missing gramine template file");
        }
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
        // Merge template with tokens
        //String sessionJsonAsString = getFilledGramineTemplate(this.gramineTemplateFilePath, enclaveEnvironments);
        SpsSessionBuilder sessionBuilder = SpsSession.builder();
        sessionBuilder.session(request.getSessionId());
        sessionBuilder.enclaves(Arrays.asList(
            toSpsSessionEnclave(enclaveEnvironments.getPreCompute()),
            toSpsSessionEnclave(enclaveEnvironments.getAppCompute()),
            toSpsSessionEnclave(enclaveEnvironments.getPostCompute())
        ));
        return sessionBuilder.build();
        /*
        try {
            return new ObjectMapper().readValue(sessionJsonAsString, SpsSession.class);
        } catch (Exception e) {
            throw new TeeSessionGenerationException(
                    TeeSessionGenerationError.SECURE_SESSION_GENERATION_FAILED,
                    "Failed to parse SPS session:" + e.getMessage());
        }
        */
    }

    private SpsSessionEnclave toSpsSessionEnclave(EnclaveEnvironment preCompute) {
        SpsSessionEnclaveBuilder enclavebuilder = SpsSessionEnclave.builder();
        enclavebuilder.name(preCompute.getName());
        enclavebuilder.mrenclave(preCompute.getMrenclave());
        enclavebuilder.environment(preCompute.getEnvironment());
        return enclavebuilder.build();
    }

    private String getFilledGramineTemplate(String templatePath, Map<String, Object> tokens) {
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        Template template = ve.getTemplate(templatePath);
        VelocityContext context = new VelocityContext();
        tokens.forEach(context::put); // copy all data from the tokens into context
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }
}
