/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.base;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.SecretUtils;
import com.iexec.sms.secret.exception.SecretAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Slf4j
public abstract class AbstractSecretController<
        S extends AbstractSecret<S, H>,
        H extends AbstractSecretHeader,
        T extends AbstractSecretService<S, H>> {
    protected final T secretService;
    protected final AuthorizationService authorizationService;

    protected AbstractSecretController(T secretService, AuthorizationService authorizationService) {
        this.secretService = secretService;
        this.authorizationService = authorizationService;
    }

    protected ResponseEntity<Void> isSecretSet(H header) {
        log.trace("Checking secret [header:{}]", header);
        final boolean exists = secretService.isSecretPresent(header);

        log.debug("Check secret [header:{}, exists:{}]", header, exists);
        return exists
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    public ResponseEntity<String> addSecret(String authorization, H header, String secretValue) {
        log.trace("Adding secret [header:{}]", header);
        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            log.trace("Failed to add secret: payload too large [header:{}]", header);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        final String challenge = getChallenge(header, secretValue);

        if (!isCorrectlySigned(challenge, authorization, header)) {
            log.error("Unauthorized to addSecret [header:{}, expectedChallenge:{}]", header, challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            secretService.addSecret(header, secretValue);
            log.debug("Added secret [header:{}]", header);
            return ResponseEntity.noContent().build();
        } catch (SecretAlreadyExistsException e) {
            log.error("Failed to add secret: already exists [header:{}]", header);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    public abstract String getChallenge(H header, String value);

    public abstract boolean isCorrectlySigned(String challenge, String authorization, H header);
}
