package com.iexec.sms.tee.session;

import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.tee.session.generic.TeeSessionStack;
import com.iexec.sms.tee.session.gramine.GramineStack;
import com.iexec.sms.tee.session.scone.SconeStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.stream.Stream;

import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeeSessionServiceTests {
    private final static String TASK_ID = "0x0";
    private final static String WORKER_ADDRESS = "0x1";
    private final static String TEE_CHALLENGE = "0x2";
    private static SconeStack sconeStack;
    private static GramineStack gramineStack;

    @Mock
    IexecHubService iexecHubService;

    TeeSessionService teeSessionService;

    @BeforeAll
    static void setUpStacks() {
        sconeStack = mock(SconeStack.class);
        when(sconeStack.getTeeEnclaveProvider()).thenReturn(TeeEnclaveProvider.SCONE);

        gramineStack = mock(GramineStack.class);
        when(gramineStack.getTeeEnclaveProvider()).thenReturn(TeeEnclaveProvider.GRAMINE);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        teeSessionService = new TeeSessionService(iexecHubService, sconeStack, gramineStack, false);
    }

    static Stream<Arguments> teeStacks() {
        return Stream.of(
                Arguments.of(sconeStack),
                Arguments.of(gramineStack)
        );
    }


    @ParameterizedTest
    @MethodSource("teeStacks")
    void shouldGenerateTeeSession(TeeSessionStack teeSessionStack) throws TeeSessionGenerationException {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeEnclaveProvider(teeSessionStack.getTeeEnclaveProvider())
                .build();
        final String sessionAsString = "session";

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(teeSessionStack.generateSession(any())).thenReturn(sessionAsString);
        when(teeSessionStack.postSession(sessionAsString.getBytes())).thenReturn(ResponseEntity.ok(null));

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
    void shouldNotGenerateTeeSessionSinceNoTeeEnclaveProvider() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeEnclaveProvider(null)
                .build();

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(TeeSessionGenerationException.class, () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(SECURE_SESSION_NO_TEE_PROVIDER, teeSessionGenerationException.getError());
        assertEquals(String.format("TEE provider can't be null [taskId:%s]",
                        TASK_ID),
                teeSessionGenerationException.getMessage());
    }


    @ParameterizedTest
    @MethodSource("teeStacks")
    void shouldNotGenerateTeeSessionSinceCantGetSession(TeeSessionStack teeSessionStack) throws TeeSessionGenerationException {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeEnclaveProvider(teeSessionStack.getTeeEnclaveProvider())
                .build();

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(teeSessionStack.generateSession(any())).thenReturn("");

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(TeeSessionGenerationException.class, () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(GET_SESSION_FAILED, teeSessionGenerationException.getError());
        assertEquals(String.format("Failed to get session [taskId:%s, workerAddress:%s, enclaveProvider:%s]",
                TASK_ID, WORKER_ADDRESS, teeSessionStack.getTeeEnclaveProvider()),
                teeSessionGenerationException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("teeStacks")
    void shouldNotGenerateTeeSessionSinceCantGenerateSecureSession(TeeSessionStack teeSessionStack) throws TeeSessionGenerationException {
        final TaskDescription taskDescription = TaskDescription.builder()
                .chainTaskId(TASK_ID)
                .teeEnclaveProvider(teeSessionStack.getTeeEnclaveProvider())
                .build();
        final String sessionAsString = "session";

        when(iexecHubService.getTaskDescription(TASK_ID)).thenReturn(taskDescription);
        when(teeSessionStack.generateSession(any())).thenReturn(sessionAsString);
        when(teeSessionStack.postSession(sessionAsString.getBytes())).thenReturn(ResponseEntity.notFound().build());

        final TeeSessionGenerationException teeSessionGenerationException = assertThrows(TeeSessionGenerationException.class, () -> teeSessionService.generateTeeSession(TASK_ID, WORKER_ADDRESS, TEE_CHALLENGE));
        assertEquals(SECURE_SESSION_STORAGE_CALL_FAILED, teeSessionGenerationException.getError());
        assertEquals(String.format("Failed to generate secure session [taskId:%s, workerAddress:%s, enclaveProvider:%s]",
                TASK_ID, WORKER_ADDRESS, teeSessionStack.getTeeEnclaveProvider()),
                teeSessionGenerationException.getMessage());
    }
}