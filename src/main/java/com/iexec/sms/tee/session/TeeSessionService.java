/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session;

import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.TeeSessionGenerationResponse;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static com.iexec.sms.api.TeeSessionGenerationError.*;

@Service
public class TeeSessionService {

    private final IexecHubService iexecHubService;
    private final TeeSessionHandler teeSessionHandler;
    private final Map<String, TeeServicesProperties> teeServicesPropertiesMap;

    public TeeSessionService(
            IexecHubService iexecService,
            TeeSessionHandler teeSessionHandler,
            Map<String, TeeServicesProperties> teeServicesPropertiesMap) {
        this.iexecHubService = iexecService;
        this.teeSessionHandler = teeSessionHandler;
        this.teeServicesPropertiesMap = teeServicesPropertiesMap;
    }

    public TeeSessionGenerationResponse generateTeeSession(
            String taskId,
            String workerAddress,
            String teeChallenge) throws TeeSessionGenerationException {
        String sessionId = createSessionId(taskId);
        TaskDescription taskDescription = iexecHubService.getTaskDescription(taskId);
        if (taskDescription == null) {
            throw new TeeSessionGenerationException(
                    GET_TASK_DESCRIPTION_FAILED,
                    String.format("Failed to get task description [taskId:%s]", taskId));
        }
        final TeeSessionRequest request;
        final TeeEnclaveConfiguration teeEnclaveConfiguration = taskDescription.getAppEnclaveConfiguration();
        if (teeEnclaveConfiguration == null) {
            throw new TeeSessionGenerationException(
                    SECURE_SESSION_NO_TEE_ENCLAVE_CONFIGURATION,
                    String.format("TEE enclave configuration can't be null [taskId:%s]", taskId));
        }

        try {
            request = TeeSessionRequest.builder()
                    .sessionId(sessionId)
                    .taskDescription(taskDescription)
                    .teeServicesProperties(resolveTeeServiceProperties(teeEnclaveConfiguration.getVersion()))
                    .workerAddress(workerAddress)
                    .enclaveChallenge(teeChallenge)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new TeeSessionGenerationException(
                    SECURE_SESSION_UNSUPPORTED_TEE_FRAMEWORK_VERSION,
                    String.format("TEE framework version unsupported [taskId:%s]", taskId));
        }

        final TeeFramework teeFramework = taskDescription.getTeeFramework();
        if (teeFramework == null) {
            throw new TeeSessionGenerationException(
                    SECURE_SESSION_NO_TEE_FRAMEWORK,
                    String.format("TEE framework can't be null [taskId:%s]", taskId));
        }

        // /!\ TODO clean expired tasks sessions
        String secretProvisioningUrl = teeSessionHandler.buildAndPostSession(request);
        return new TeeSessionGenerationResponse(sessionId, secretProvisioningUrl);
    }

    public TeeServicesProperties resolveTeeServiceProperties(String version) {
        if (version == null) {
            return teeServicesPropertiesMap.values().iterator().next();
        }

        return Optional.ofNullable(teeServicesPropertiesMap.get(version))
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("SMS is not configured to use required framework version [required:%s, supported:%s]",
                                version, teeServicesPropertiesMap.keySet())));
    }

    private String createSessionId(String taskId) {
        String randomString = RandomStringUtils.randomAlphanumeric(10);
        return String.format("%s0000%s", randomString, taskId);
    }

}
