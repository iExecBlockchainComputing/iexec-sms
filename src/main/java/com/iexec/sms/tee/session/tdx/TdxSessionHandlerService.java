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

import com.iexec.common.utils.FeignBuilder;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.ssl.SslConfig;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionHandler;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import com.iexec.sms.tee.session.tdx.storage.TdxSession;
import com.iexec.sms.tee.session.tdx.storage.TdxSessionStorageApiClient;
import com.iexec.sms.tee.session.tdx.storage.TdxSessionStorageConfiguration;
import feign.Client;
import feign.Logger;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnTeeFramework(frameworks = TeeFramework.TDX)
public class TdxSessionHandlerService implements TeeSessionHandler {
    private final TdxSessionMakerService sessionService;
    private final TdxSessionStorageConfiguration storageConfiguration;
    private final TdxSessionStorageApiClient storageClient;

    public TdxSessionHandlerService(
            final SslConfig sslConfig,
            final TdxSessionMakerService sessionService,
            final TdxSessionStorageConfiguration storageConfiguration) {
        this.sessionService = sessionService;
        this.storageConfiguration = storageConfiguration;
        this.storageClient = FeignBuilder.createBuilder(Logger.Level.FULL)
                .client(new Client.Default(sslConfig.getFreshSslContext().getSocketFactory(), NoopHostnameVerifier.INSTANCE))
                .target(TdxSessionStorageApiClient.class, storageConfiguration.getPostUrl());
    }

    /**
     * Build and post secret session on secret provisioning service.
     *
     * @param request tee session generation request
     * @return String secret provisioning service url
     * @throws TeeSessionGenerationException if an error occurs
     */
    @Override
    public String buildAndPostSession(final TeeSessionRequest request) throws TeeSessionGenerationException {
        final TdxSession session = sessionService.generateSession(request);

        try {
            storageClient.postSession(session);
            return storageConfiguration.getRemoteAttestationUrl();
        } catch (Exception e) {
            throw new TeeSessionGenerationException(
                    TeeSessionGenerationError.SECURE_SESSION_STORAGE_CALL_FAILED,
                    "Failed to post session: " + e.getMessage());
        }
    }
}
