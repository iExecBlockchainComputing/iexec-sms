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
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.api.TeeSessionGenerationResponse;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeeSessionServiceTests {
    private static final String TASK_ID = "0x0";
    private static final String WORKER_ADDRESS = "0x1";
    private static final String TEE_CHALLENGE = "0x2";
    private static final String SECRET_PROVISIONING_URL = "https://secretProvisioningUrl";
    private static final String VERSION = "v5";
    private static final String LAS_IMAGE = "lasImage";
    @Mock
    private IexecHubService iexecHubService;
    @Mock
    private TeeSessionHandler teeSessionHandler;

    final TeeAppProperties preComputeProperties = TeeAppProperties.builder()
            .image("pre-image")
            .fingerprint("pre-fingerprint")
            .heapSizeInBytes(3L)
            .entrypoint("pre-entrypoint")
            .build();
    final TeeAppProperties postComputeProperties = TeeAppProperties.builder()
            .image("post-image")
            .fingerprint("post-fingerprint")
            .heapSizeInBytes(5L)
            .entrypoint("post-entrypoint")
            .build();

    private final TeeServicesProperties teeServicesProperties = new SconeServicesProperties(VERSION, preComputeProperties, postComputeProperties, LAS_IMAGE);
    private TeeSessionService teeSessionService;
    Map<String, TeeServicesProperties> teeServicesPropertiesMap;

    @BeforeEach
    void setUp() {
        teeServicesPropertiesMap = Map.of(VERSION, teeServicesProperties);
        teeSessionService = new TeeSessionService(iexecHubService, teeSessionHandler, teeServicesPropertiesMap);
    }

    @Test
    void shouldGenerateSconeSession()
            throws TeeSessionGenerationException {

        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeFramework(TeeFramework.SCONE)
                .appEnclaveConfiguration(
                        TeeEnclaveConfiguration.builder()
                                .version(VERSION)
                                .build()
                )
                .build();
        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(teeSessionHandler.buildAndPostSession(any(TeeSessionRequest.class))).thenReturn(SECRET_PROVISIONING_URL);

        final TeeSessionGenerationResponse teeSessionReponse = assertDoesNotThrow(
                () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        verify(teeSessionHandler, times(1))
                .buildAndPostSession(any());
        assertFalse(teeSessionReponse.getSessionId().isEmpty());
        assertEquals(SECRET_PROVISIONING_URL, teeSessionReponse.getSecretProvisioningUrl());
    }

    @Test
    void shouldGenerateGramineSession()
            throws TeeSessionGenerationException {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeFramework(TeeFramework.GRAMINE)
                .appEnclaveConfiguration(
                        TeeEnclaveConfiguration.builder()
                                .version(VERSION)
                                .build()
                )
                .build();
        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(teeSessionHandler.buildAndPostSession(any(TeeSessionRequest.class))).thenReturn(SECRET_PROVISIONING_URL);

        final TeeSessionGenerationResponse teeSessionReponse = assertDoesNotThrow(
                () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        verify(teeSessionHandler, times(1))
                .buildAndPostSession(any());
        assertFalse(teeSessionReponse.getSessionId().isEmpty());
        assertEquals(SECRET_PROVISIONING_URL, teeSessionReponse.getSecretProvisioningUrl());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceCantGetTaskDescription() {
        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(null);

        final TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class, () ->
                teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(GET_TASK_DESCRIPTION_FAILED, exception.getError());
        assertEquals(String.format("Failed to get task description [taskId:%s]", TASK_ID), exception.getMessage());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceTeeEnclaveConfigurationIsNull() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeFramework(TeeFramework.GRAMINE)
                .appEnclaveConfiguration(null)
                .build();
        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);

        final TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class, () ->
                teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(SECURE_SESSION_NO_TEE_ENCLAVE_CONFIGURATION, exception.getError());
        assertEquals(String.format("TEE enclave configuration can't be null [taskId:%s]", TASK_ID), exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenVersionUnsupported() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeFramework(TeeFramework.GRAMINE)
                .appEnclaveConfiguration(
                        TeeEnclaveConfiguration.builder()
                                .version("v1.2.3")
                                .build()
                )
                .build();
        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);

        final TeeSessionGenerationException exception = assertThrows(TeeSessionGenerationException.class, () ->
                teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(SECURE_SESSION_UNSUPPORTED_TEE_FRAMEWORK_VERSION, exception.getError());
        assertEquals(String.format("TEE framework version unsupported [taskId:%s]", TASK_ID), exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTeeFrameworkIsNull() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeFramework(null)
                .appEnclaveConfiguration(
                        TeeEnclaveConfiguration.builder()
                                .version(VERSION)
                                .build()
                )
                .build();

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(TeeSessionGenerationError.SECURE_SESSION_NO_TEE_FRAMEWORK,
                teeSessionGenerationException.getError());
        assertEquals(String.format("TEE framework can't be null [taskId:%s]", TASK_ID),
                teeSessionGenerationException.getMessage());
    }

    @Test
    void testResolveTeeServicePropertiesWithNullVersion() {
        final TeeServicesProperties result = teeSessionService.resolveTeeServiceProperties(null);
        assertNotNull(result);
        assertTrue(teeServicesPropertiesMap.containsValue(result));
    }

    @Test
    void testResolveTeeServicePropertiesWithExistingVersion() {
        final TeeServicesProperties result = teeSessionService.resolveTeeServiceProperties(VERSION);

        assertNotNull(result);
        assertEquals(VERSION, result.getTeeFrameworkVersion());
        assertEquals(TeeFramework.SCONE, result.getTeeFramework());
        assertEquals(preComputeProperties, result.getPreComputeProperties());
        assertEquals(postComputeProperties, result.getPostComputeProperties());
    }

    @Test
    void testResolveTeeServicePropertiesWithNonExistingVersion() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            teeSessionService.resolveTeeServiceProperties("v3");
        });

        final String expectedMessagePart = "SMS is not configured to use required framework version [required:v3, supported:[v5]]";
        assertEquals(expectedMessagePart, exception.getMessage());
    }

}
