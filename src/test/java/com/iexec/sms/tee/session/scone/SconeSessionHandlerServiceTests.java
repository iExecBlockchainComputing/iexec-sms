package com.iexec.sms.tee.session.scone;

import com.iexec.common.task.TaskDescription;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.tee.session.TeeSessionLogConfiguration;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.scone.cas.CasClient;
import com.iexec.sms.tee.session.scone.cas.CasConfiguration;
import com.iexec.sms.tee.session.scone.cas.SconeSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
public class SconeSessionHandlerServiceTests {

    private static final String CAS_URL = "casUrl";
    @Mock
    private SconeSessionMakerService sessionService;
    @Mock
    private CasClient apiClient;
    @Mock
    private TeeSessionLogConfiguration teeSessionLogConfiguration;
    @Mock
    private CasConfiguration casConfiguration;
    @InjectMocks
    private SconeSessionHandlerService sessionHandlerService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        when(casConfiguration.getEnclaveUrl()).thenReturn(CAS_URL);
    }

    @Test
    void shouldBuildAndPostSession(CapturedOutput output) throws TeeSessionGenerationException {
        TeeSessionRequest request = mock(TeeSessionRequest.class);
        TaskDescription taskDescription = mock(TaskDescription.class);
        when(request.getTaskDescription()).thenReturn(taskDescription);
        SconeSession casSession = mock(SconeSession.class);
        when(casSession.toString()).thenReturn("sessionContent");
        when(sessionService.generateSession(request)).thenReturn(casSession);
        when(teeSessionLogConfiguration.isDisplayDebugSessionEnabled()).thenReturn(true);
        when(apiClient.postSession(casSession.toString())).thenReturn(ResponseEntity.ok("sessionId"));
        
        assertEquals(CAS_URL, sessionHandlerService.buildAndPostSession(request));
        // Testing output here since it reflects a business feature (ability to catch a
        // session in debug mode)
        assertTrue(output.getOut().contains("Session content [taskId:null]\nsessionContent\n"));
    }

    @Test
    void shouldNotBuildAndPostSessionSinceBuildSessionFailed()
            throws TeeSessionGenerationException {
        TeeSessionRequest request = mock(TeeSessionRequest.class);
        TeeSessionGenerationException teeSessionGenerationException = new TeeSessionGenerationException(
                TeeSessionGenerationError.SECURE_SESSION_GENERATION_FAILED, "some error");
        when(sessionService.generateSession(request)).thenThrow(teeSessionGenerationException);

        assertThrows(teeSessionGenerationException.getClass(),
                () -> sessionHandlerService.buildAndPostSession(request));
    }

    @Test
    void shouldNotBuildAndPostSessionSincePostSessionFailed()
            throws TeeSessionGenerationException {
        TeeSessionRequest request = mock(TeeSessionRequest.class);
        TaskDescription taskDescription = mock(TaskDescription.class);
        when(request.getTaskDescription()).thenReturn(taskDescription);
        SconeSession casSession = mock(SconeSession.class);
        when(sessionService.generateSession(request)).thenReturn(casSession);
        when(teeSessionLogConfiguration.isDisplayDebugSessionEnabled()).thenReturn(true);
        when(apiClient.postSession(casSession.toString())).thenReturn(ResponseEntity.internalServerError().build());

        assertThrows(TeeSessionGenerationException.class,
                () -> sessionHandlerService.buildAndPostSession(request));
    }
}
