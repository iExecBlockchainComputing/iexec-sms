/*
 * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
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
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.api.TeeSessionGenerationResponse;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.gramine.GramineSessionHandlerService;
import com.iexec.sms.tee.session.scone.SconeSessionHandlerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeeSessionServiceTests {
    private static final String TASK_ID = "0x0";
    private static final String WORKER_ADDRESS = "0x1";
    private static final String TEE_CHALLENGE = "0x2";
    private static final String SECRET_PROVISIONING_URL = "https://secretProvisioningUrl";
    @Mock
    private SconeSessionHandlerService sconeService;
    @Mock
    private GramineSessionHandlerService gramineService;
    @Mock
    private IexecHubService iexecHubService;

    @Test
    void shouldGenerateSconeSession()
            throws TeeSessionGenerationException {
        final TeeSessionService teeSessionService = new TeeSessionService(iexecHubService, sconeService);

        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeFramework(TeeFramework.SCONE)
                .build();
        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(sconeService.buildAndPostSession(any())).thenReturn(SECRET_PROVISIONING_URL);

        final TeeSessionGenerationResponse teeSessionReponse = assertDoesNotThrow(
                () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        verify(sconeService, times(1))
                .buildAndPostSession(any());
        assertFalse(teeSessionReponse.getSessionId().isEmpty());
        assertEquals(SECRET_PROVISIONING_URL, teeSessionReponse.getSecretProvisioningUrl());
    }

    @Test
    void shouldGenerateGramineSession()
            throws TeeSessionGenerationException {
        final TeeSessionService teeSessionService = new TeeSessionService(iexecHubService, gramineService);

        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeFramework(TeeFramework.GRAMINE)
                .build();
        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(gramineService.buildAndPostSession(any())).thenReturn(SECRET_PROVISIONING_URL);

        final TeeSessionGenerationResponse teeSessionReponse = assertDoesNotThrow(
                () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        verify(gramineService, times(1))
                .buildAndPostSession(any());
        assertFalse(teeSessionReponse.getSessionId().isEmpty());
        assertEquals(SECRET_PROVISIONING_URL, teeSessionReponse.getSecretProvisioningUrl());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceCantGetTaskDescription() {
        final TeeSessionService teeSessionService = new TeeSessionService(iexecHubService, sconeService);

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(null);

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(TeeSessionGenerationError.GET_TASK_DESCRIPTION_FAILED,
                teeSessionGenerationException.getError());
        assertEquals(String.format("Failed to get task description [taskId:%s]", TASK_ID),
                teeSessionGenerationException.getMessage());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceNoTeeFramework() {
        final TeeSessionService teeSessionService = new TeeSessionService(iexecHubService, sconeService);

        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeFramework(null)
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

}