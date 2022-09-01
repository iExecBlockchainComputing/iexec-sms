package com.iexec.sms.tee.session;

import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.api.TeeSessionGenerationResponse;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.gramine.GramineSessionHandlerService;
import com.iexec.sms.tee.session.scone.SconeSessionHandlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeeSessionServiceTests {
    private final static String TASK_ID = "0x0";
    private final static String WORKER_ADDRESS = "0x1";
    private final static String TEE_CHALLENGE = "0x2";
    private static final String SECRET_PROVISIONING_URL = "https://secretProvisioningUrl";
    @Mock
    private SconeSessionHandlerService sconeService;
    @Mock
    private GramineSessionHandlerService gramineService;
    @Mock
    private IexecHubService iexecHubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldGenerateSconeSession()
            throws TeeSessionGenerationException {
        final TeeSessionService teeSessionService = new TeeSessionService(iexecHubService, sconeService);

        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeEnclaveProvider(TeeEnclaveProvider.SCONE)
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
                .teeEnclaveProvider(TeeEnclaveProvider.GRAMINE)
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
    void shouldNotGenerateTeeSessionSinceNoTeeEnclaveProvider() {
        final TeeSessionService teeSessionService = new TeeSessionService(iexecHubService, sconeService);

        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeEnclaveProvider(null)
                .build();

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(
                TeeSessionGenerationException.class,
                () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(TeeSessionGenerationError.SECURE_SESSION_NO_TEE_PROVIDER,
                teeSessionGenerationException.getError());
        assertEquals(String.format("TEE provider can't be null [taskId:%s]",
                TASK_ID),
                teeSessionGenerationException.getMessage());
    }

}