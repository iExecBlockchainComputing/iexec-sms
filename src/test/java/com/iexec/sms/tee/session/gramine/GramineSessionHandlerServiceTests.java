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

package com.iexec.sms.tee.session.gramine;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.sms.MemoryLogAppender;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.gramine.sps.GramineSession;
import com.iexec.sms.tee.session.gramine.sps.SpsApiClient;
import com.iexec.sms.tee.session.gramine.sps.SpsConfiguration;
import feign.FeignException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.createSessionRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GramineSessionHandlerServiceTests {

    private static final String SPS_URL = "spsUrl";
    @Mock
    private GramineSessionMakerService sessionService;
    @Mock
    private SpsConfiguration spsConfiguration;
    @InjectMocks
    private GramineSessionHandlerService sessionHandlerService;
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
        when(spsConfiguration.getEnclaveHost()).thenReturn(SPS_URL);
        memoryLogAppender.reset();
    }

    @Test
    void shouldBuildAndPostSession() throws TeeSessionGenerationException {
        TaskDescription taskDescription = TaskDescription.builder().build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        GramineSession spsSession = mock(GramineSession.class);
        when(spsSession.toString()).thenReturn("sessionContent");
        when(sessionService.generateSession(request)).thenReturn(spsSession);
        SpsApiClient spsClient = mock(SpsApiClient.class);
        when(spsClient.postSession(spsSession)).thenReturn("sessionId");
        when(spsConfiguration.getInstance()).thenReturn(spsClient);
        assertEquals(SPS_URL, sessionHandlerService.buildAndPostSession(request));
        assertTrue(memoryLogAppender.isEmpty());
    }

    @Test
    void shouldNotBuildAndPostSessionSinceBuildSessionFailed()
            throws TeeSessionGenerationException {
        TeeSessionRequest request = TeeSessionRequest.builder().build();
        TeeSessionGenerationException teeSessionGenerationException = new TeeSessionGenerationException(
                TeeSessionGenerationError.SECURE_SESSION_GENERATION_FAILED, "some error");
        when(sessionService.generateSession(request)).thenThrow(teeSessionGenerationException);

        assertThrows(teeSessionGenerationException.getClass(),
                () -> sessionHandlerService.buildAndPostSession(request));
    }

    @Test
    void shouldNotBuildAndPostSessionSincePostSessionFailed()
            throws TeeSessionGenerationException {
        TaskDescription taskDescription = TaskDescription.builder().build();
        TeeSessionRequest request = createSessionRequest(taskDescription);
        GramineSession spsSession = mock(GramineSession.class);
        when(spsSession.toString()).thenReturn("sessionContent");
        when(sessionService.generateSession(request)).thenReturn(spsSession);
        SpsApiClient spsClient = mock(SpsApiClient.class);
        when(spsConfiguration.getInstance()).thenReturn(spsClient);
        FeignException apiClientException = mock(FeignException.class);
        when(spsClient.postSession(spsSession)).thenThrow(apiClientException);

        assertThrows(TeeSessionGenerationException.class,
                () -> sessionHandlerService.buildAndPostSession(request));
    }

}
