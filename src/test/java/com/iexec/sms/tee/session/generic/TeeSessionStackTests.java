package com.iexec.sms.tee.session.generic;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.tee.session.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.TeeSessionGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeeSessionStackTests {
    private final TeeEnclaveProvider teeEnclaveProvider = TeeEnclaveProvider.GRAMINE;
    @Mock
    private TeeSessionProviderService sessionService;
    @Mock
    private TeeSessionStorageClient client;

    private TeeSessionStack teeSessionStack;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        teeSessionStack = new TeeSessionStack(teeEnclaveProvider, sessionService, client) {};
    }

    // region generateSession
    @Test
    void shouldGenerateSession() throws TeeSessionGenerationException {
        TeeSecretsSessionRequest request = mock(TeeSecretsSessionRequest.class);
        assertDoesNotThrow(() -> teeSessionStack.generateSession(request));
        verify(sessionService, times(1)).generateSession(request);
        verify(client, times(0)).postSession(any());
    }

    @Test
    void shouldNotGenerateSessionSinceExceptionThrown() throws TeeSessionGenerationException {
        TeeSecretsSessionRequest request = mock(TeeSecretsSessionRequest.class);
        final TeeSessionGenerationException expectedException =
                new TeeSessionGenerationException(TeeSessionGenerationError.APP_COMPUTE_INVALID_ENCLAVE_CONFIG, "");
        when(sessionService.generateSession(request)).thenThrow(expectedException);

        final TeeSessionGenerationException exception =
                assertThrows(TeeSessionGenerationException.class, () -> teeSessionStack.generateSession(request));
        assertEquals(expectedException, exception);
        verify(sessionService, times(1)).generateSession(request);
        verify(client, times(0)).postSession(any());
    }
    // endregion

    // region postSession
    @Test
    void shouldPostSession() throws TeeSessionGenerationException {
        byte[] sessionFile = new byte[10];
        assertDoesNotThrow(() -> teeSessionStack.postSession(sessionFile));
        verify(sessionService, times(0)).generateSession(any());
        verify(client, times(1)).postSession(sessionFile);
    }

    @Test
    void shouldNotPostSessionSinceExceptionThrown() throws TeeSessionGenerationException {
        byte[] sessionFile = new byte[10];
        final RuntimeException expectedException = new RuntimeException();
        when(client.postSession(sessionFile)).thenThrow(expectedException);

        final RuntimeException exception =
                assertThrows(RuntimeException.class, () -> teeSessionStack.postSession(sessionFile));
        assertEquals(expectedException, exception);
        verify(sessionService, times(0)).generateSession(any());
        verify(client, times(1)).postSession(sessionFile);
    }
    // endregion
}