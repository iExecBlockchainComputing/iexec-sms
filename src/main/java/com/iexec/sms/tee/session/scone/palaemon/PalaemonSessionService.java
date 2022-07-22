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

package com.iexec.sms.tee.session.scone.palaemon;

import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.utils.FileHelper;
import com.iexec.sms.tee.session.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSecretsService;
import com.iexec.sms.tee.session.scone.attestation.AttestationSecurityConfig;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

//TODO Rename and move
@Slf4j
@Service
public class PalaemonSessionService {

    // Internal values required for setting up a palaemon session
    // Generic
    static final String TOLERATED_INSECURE_OPTIONS = "TOLERATED_INSECURE_OPTIONS";
    static final String IGNORED_SGX_ADVISORIES = "IGNORED_SGX_ADVISORIES";
    static final String APP_ARGS = "APP_ARGS";

    // PreCompute
    static final String PRE_COMPUTE_ENTRYPOINT = "PRE_COMPUTE_ENTRYPOINT";
    // PostCompute
    static final String POST_COMPUTE_ENTRYPOINT = "POST_COMPUTE_ENTRYPOINT";

    private final TeeSecretsService teeSecretsService;
    private final TeeWorkflowConfiguration teeWorkflowConfig;
    private final AttestationSecurityConfig attestationSecurityConfig;

    @Value("${scone.cas.palaemon}")
    private String palaemonTemplateFilePath;

    public PalaemonSessionService(
            TeeSecretsService teeSecretsService,
            TeeWorkflowConfiguration teeWorkflowConfig,
            AttestationSecurityConfig attestationSecurityConfig) {
        this.teeSecretsService = teeSecretsService;
        this.teeWorkflowConfig = teeWorkflowConfig;
        this.attestationSecurityConfig = attestationSecurityConfig;
    }

    @PostConstruct
    void postConstruct() throws FileNotFoundException {
        if (StringUtils.isEmpty(palaemonTemplateFilePath)) {
            throw new IllegalArgumentException("Missing palaemon template filepath");
        }
        if (!FileHelper.exists(palaemonTemplateFilePath)) {
            throw new FileNotFoundException("Missing palaemon template file");
        }
    }

    /**
     * Collect tokens required for different compute stages (pre, in, post)
     * and build the yaml config of the TEE session.
     * <p>
     * TODO: Read onchain available infos from enclave instead of copying
     * public vars to palaemon.yml. It needs ssl call from enclave to eth
     * node (only ethereum node address required inside palaemon.yml)
     *
     * @param request session request details
     * @return session config in yaml string format
     */
    public String generateSession(TeeSecretsSessionRequest request) throws TeeSessionGenerationException {
        Map<String, Object> tokens = teeSecretsService.getSecretsTokens(request);
        tokens.putAll(getSpecificPalaemonTokens(request));
        // Merge template with tokens and return the result
        return getFilledPalaemonTemplate(this.palaemonTemplateFilePath, tokens);
    }

    Map<String, Object> getSpecificPalaemonTokens(TeeSecretsSessionRequest request) {
        Map<String, Object> tokens = new HashMap<>();

        // Add entrypoints
        tokens.put(PRE_COMPUTE_ENTRYPOINT, teeWorkflowConfig.getPreComputeEntrypoint());
        tokens.put(POST_COMPUTE_ENTRYPOINT, teeWorkflowConfig.getPostComputeEntrypoint());

        // Add attestation security config
        String toleratedInsecureOptions =
                String.join(",", attestationSecurityConfig.getToleratedInsecureOptions());
        String ignoredSgxAdvisories =
                String.join(",", attestationSecurityConfig.getIgnoredSgxAdvisories());
        tokens.put(TOLERATED_INSECURE_OPTIONS, toleratedInsecureOptions);
        tokens.put(IGNORED_SGX_ADVISORIES, ignoredSgxAdvisories);

        // Add app args
        TaskDescription taskDescription = request.getTaskDescription();
        TeeEnclaveConfiguration enclaveConfig = taskDescription.getAppEnclaveConfiguration();
        String appArgs = enclaveConfig.getEntrypoint();
        if (!StringUtils.isEmpty(taskDescription.getCmd())) {
            appArgs = appArgs + " " + taskDescription.getCmd();
        }
        tokens.put(APP_ARGS, appArgs);

        return tokens;
    }

    private String getFilledPalaemonTemplate(String templatePath, Map<String, Object> tokens) {
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
