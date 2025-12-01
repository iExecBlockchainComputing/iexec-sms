/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.tdx;

import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.ssl.SslConfig;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.tdx.storage.TdxSession;
import com.iexec.sms.tee.session.tdx.storage.TdxSessionStorageApiClient;
import com.iexec.sms.tee.session.tdx.storage.TdxSessionStorageConfiguration;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

import static com.iexec.sms.tee.session.TeeSessionTestUtils.createSessionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TdxSessionHandlerServiceTests {
    private static final String STORAGE_SESSION_URL = "storageSessionUrl";
    @Mock
    private SslConfig sslConfig;
    @Mock
    private TdxSessionMakerService sessionService;
    @Mock
    private TdxSessionStorageConfiguration storageConfiguration;
    private TdxSessionHandlerService sessionHandlerService;

    @Mock
    private TdxSessionStorageApiClient storageClient;

    private void setupMockService() throws NoSuchAlgorithmException {
        when(sslConfig.getFreshSslContext()).thenReturn(SSLContext.getDefault());
        when(storageConfiguration.getPostUrl()).thenReturn("http://session-storage");
        sessionHandlerService = new TdxSessionHandlerService(sslConfig, sessionService, storageConfiguration);
        ReflectionTestUtils.setField(sessionHandlerService, "storageClient", storageClient);
    }

    @Test
    void shouldBuildAndPostSession() throws TeeSessionGenerationException, NoSuchAlgorithmException {
        setupMockService();
        final TaskDescription taskDescription = TaskDescription.builder().build();
        final TeeSessionRequest request = createSessionRequest(taskDescription);
        final TdxSession tdxSession = mock(TdxSession.class);
        when(sessionService.generateSession(request)).thenReturn(tdxSession);
        when(storageConfiguration.getRemoteAttestationUrl()).thenReturn(STORAGE_SESSION_URL);
        assertThat(sessionHandlerService.buildAndPostSession(request)).isEqualTo(STORAGE_SESSION_URL);
        verify(storageClient).postSession(tdxSession);
    }

    @Test
    void shouldNotBuildAndPostSessionSinceBuildSessionFailed() throws TeeSessionGenerationException, NoSuchAlgorithmException {
        setupMockService();
        final TeeSessionRequest request = TeeSessionRequest.builder().build();
        TeeSessionGenerationException teeSessionGenerationException = new TeeSessionGenerationException(
                TeeSessionGenerationError.SECURE_SESSION_GENERATION_FAILED, "some error");
        when(sessionService.generateSession(request)).thenThrow(teeSessionGenerationException);
        assertThatThrownBy(() -> sessionHandlerService.buildAndPostSession(request))
                .isInstanceOf(TeeSessionGenerationException.class)
                .hasFieldOrPropertyWithValue("error", TeeSessionGenerationError.SECURE_SESSION_GENERATION_FAILED);
    }

    @Test
    void shouldNotBuildAndPostSessionSincePostSessionFailed() throws TeeSessionGenerationException, NoSuchAlgorithmException {
        setupMockService();
        final TeeSessionRequest request = TeeSessionRequest.builder().build();
        final TdxSession tdxSession = mock(TdxSession.class);
        when(sessionService.generateSession(request)).thenReturn(tdxSession);
        when(storageClient.postSession(tdxSession)).thenThrow(FeignException.class);
        assertThatThrownBy(() -> sessionHandlerService.buildAndPostSession(request))
                .isInstanceOf(TeeSessionGenerationException.class)
                .hasFieldOrPropertyWithValue("error", TeeSessionGenerationError.SECURE_SESSION_STORAGE_CALL_FAILED);
    }
}
