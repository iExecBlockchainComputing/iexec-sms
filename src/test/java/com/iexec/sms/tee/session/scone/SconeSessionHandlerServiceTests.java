/*
 * Copyright 2022-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.scone;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.sms.MemoryLogAppender;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.scone.cas.CasClient;
import com.iexec.sms.tee.session.scone.cas.CasConfiguration;
import com.iexec.sms.tee.session.scone.cas.SconeSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.createSessionRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SconeSessionHandlerServiceTests {

    private static final String CAS_URL = "casUrl";
    @Mock
    private SconeSessionMakerService sessionService;
    @Mock
    private CasClient apiClient;
    @Mock
    private CasConfiguration casConfiguration;
    @InjectMocks
    private SconeSessionHandlerService sessionHandlerService;

    private static MemoryLogAppender memoryLogAppender;

    @BeforeAll
    static void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        memoryLogAppender = new MemoryLogAppender();
        memoryLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryLogAppender);
        memoryLogAppender.start();
    }

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        when(casConfiguration.getEnclaveHost()).thenReturn(CAS_URL);
        memoryLogAppender.reset();
    }

    @Test
    void shouldBuildAndPostSessionWithLogs()
            throws TeeSessionGenerationException {
        TaskDescription taskDescription = TaskDescription.builder().build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        SconeSession casSession = mock(SconeSession.class);
        when(casSession.toString()).thenReturn("sessionContent");
        when(sessionService.generateSession(request)).thenReturn(casSession);
        when(apiClient.postSession(casSession.toString()))
                .thenReturn(ResponseEntity.created(null).body("sessionId"));

        assertEquals(CAS_URL,
                sessionHandlerService.buildAndPostSession(request));
        assertTrue(memoryLogAppender.isEmpty());
    }

    @Test
    void shouldBuildAndPostSessionWithoutLogs()
            throws TeeSessionGenerationException {
        TaskDescription taskDescription = TaskDescription.builder().build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        SconeSession casSession = mock(SconeSession.class);
        when(casSession.toString()).thenReturn("sessionContent");
        when(sessionService.generateSession(request)).thenReturn(casSession);
        when(apiClient.postSession(casSession.toString()))
                .thenReturn(ResponseEntity.created(null).body("sessionId"));

        assertEquals(CAS_URL,
                sessionHandlerService.buildAndPostSession(request));
        assertTrue(memoryLogAppender.isEmpty());
    }

    @Test
    void shouldNotBuildAndPostSessionSinceBuildSessionFailed()
            throws TeeSessionGenerationException {
        TeeSessionRequest request = TeeSessionRequest.builder().build();
        TeeSessionGenerationException teeSessionGenerationException = new TeeSessionGenerationException(
                TeeSessionGenerationError.SECURE_SESSION_GENERATION_FAILED,
                "some error");
        when(sessionService.generateSession(request))
                .thenThrow(teeSessionGenerationException);

        assertThrows(teeSessionGenerationException.getClass(),
                () -> sessionHandlerService.buildAndPostSession(request));
    }

    @Test
    void shouldNotBuildAndPostSessionSincePostSessionFailed()
            throws TeeSessionGenerationException {
        TaskDescription taskDescription = TaskDescription.builder().build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        SconeSession casSession = mock(SconeSession.class);
        when(sessionService.generateSession(request)).thenReturn(casSession);
        when(apiClient.postSession(casSession.toString()))
                .thenReturn(ResponseEntity.internalServerError().build());

        assertThrows(TeeSessionGenerationException.class,
                () -> sessionHandlerService.buildAndPostSession(request));
    }

    @Test
    void shouldNotBuildAndPostSessionSinceNoResponse()
            throws TeeSessionGenerationException {
        TaskDescription taskDescription = TaskDescription.builder().build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        SconeSession casSession = mock(SconeSession.class);
        when(sessionService.generateSession(request)).thenReturn(casSession);
        when(apiClient.postSession(casSession.toString()))
                .thenReturn(null);

        assertThrows(TeeSessionGenerationException.class,
                () -> sessionHandlerService.buildAndPostSession(request));
    }
}
