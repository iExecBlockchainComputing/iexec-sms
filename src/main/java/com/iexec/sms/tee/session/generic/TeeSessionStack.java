/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.generic;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.tee.session.TeeSecretsSessionRequest;
import com.iexec.sms.tee.session.TeeSessionGenerationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

/**
 * Simple class grouping services & clients
 * related to a single TEE enclave provider.
 * Some methods from these services & clients
 * are transparently called from this class' methods.
 */
@AllArgsConstructor
@Getter
public abstract class TeeSessionStack {
    private final TeeEnclaveProvider teeEnclaveProvider;
    private final TeeSessionProviderService sessionService;
    private final TeeSessionStorageClient client;

    public String generateSession(TeeSecretsSessionRequest request) throws TeeSessionGenerationException {
        return sessionService.generateSession(request);
    }

    public ResponseEntity<String> postSession(byte[] sessionFile) {
        return client.postSession(sessionFile);
    }
}
