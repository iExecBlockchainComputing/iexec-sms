package com.iexec.sms.tee.session;

import com.iexec.common.task.TaskDescription;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.cas.CasClient;
import com.iexec.sms.tee.session.palaemon.PalaemonSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static com.iexec.common.tee.TeeSessionGenerationError.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TeeSessionServiceTests {
    private final static String TASK_ID = "0x0";
    private final static String WORKER_ADDRESS = "0x1";
    private final static String TEE_CHALLENGE = "0x2";

    @Mock
    IexecHubService iexecHubService;

    @Mock
    CasClient casClient;

    @Mock
    PalaemonSessionService palaemonSessionService;

    TeeSessionService teeSessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        teeSessionService = new TeeSessionService(iexecHubService, palaemonSessionService, casClient, false);
    }

    @Test
    void shouldGenerateTeeSession() throws TeeSessionGenerationException {
        final TaskDescription taskDescription = TaskDescription.builder().chainTaskId(TASK_ID).build();
        final String sessionYmlAsString = "YML session";

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(palaemonSessionService.getSessionYml(any())).thenReturn(sessionYmlAsString);
        when(casClient.generateSecureSession(sessionYmlAsString.getBytes())).thenReturn(ResponseEntity.ok(null));

        final String teeSession = assertDoesNotThrow(() -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertNotNull(teeSession);
    }

    @Test
    void shouldNotGenerateTeeSessionSinceCantGetTaskDescription() {
        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(null);

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(TeeSessionGenerationException.class, () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(GET_TASK_DESCRIPTION_FAILED, teeSessionGenerationException.getError());
        assertEquals(String.format("Failed to get task description [taskId:%s]", TASK_ID), teeSessionGenerationException.getMessage());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceCantGetSessionYml() throws TeeSessionGenerationException {
        final TaskDescription taskDescription = TaskDescription.builder().chainTaskId(TASK_ID).build();

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(palaemonSessionService.getSessionYml(any())).thenReturn("");

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(TeeSessionGenerationException.class, () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(GET_SESSION_YML_FAILED, teeSessionGenerationException.getError());
        assertEquals(String.format("Failed to get session yml [taskId:%s, workerAddress:%s]", TASK_ID, WORKER_ADDRESS), teeSessionGenerationException.getMessage());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceCantGenerateSecureSession() throws TeeSessionGenerationException {
        final TaskDescription taskDescription = TaskDescription.builder().chainTaskId(TASK_ID).build();
        final String sessionYmlAsString = "YML session";

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(palaemonSessionService.getSessionYml(any())).thenReturn(sessionYmlAsString);
        when(casClient.generateSecureSession(sessionYmlAsString.getBytes())).thenReturn(ResponseEntity.notFound().build());

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(TeeSessionGenerationException.class, () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(SECURE_SESSION_GENERATION_FAILED, teeSessionGenerationException.getError());
        assertEquals(String.format("Failed to generate secure session [taskId:%s, workerAddress:%s]", TASK_ID, WORKER_ADDRESS), teeSessionGenerationException.getMessage());
    }
}