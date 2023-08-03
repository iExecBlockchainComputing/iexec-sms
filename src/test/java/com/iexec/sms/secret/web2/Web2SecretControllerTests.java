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

package com.iexec.sms.secret.web2;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.exception.NotAnExistingSecretException;
import com.iexec.sms.secret.exception.SameSecretException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class Web2SecretControllerTests {
    private static final String AUTHORIZATION = "AUTHORIZATION";
    private static final String CHALLENGE = "CHALLENGE";
    private static final String OWNER_ADDRESS = "OWNER_ADDRESS".toLowerCase();
    private static final String SECRET_NAME = "SECRET_NAME".toLowerCase();
    private static final String SECRET_VALUE = "SECRET_VALUE";
    private static final Web2SecretHeader SECRET_HEADER = new Web2SecretHeader(OWNER_ADDRESS, SECRET_NAME);

    private static final SecureRandom seed = new SecureRandom();

    @Mock
    private Web2SecretService secretService;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private Web2SecretController secretController;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    //region updateWeb2Secret
    @Test
    void failToUpdateWeb2SecretWhenPayloadTooLarge() {
        assertThat(secretController.updateWeb2Secret(AUTHORIZATION, OWNER_ADDRESS, SECRET_NAME, getRandomString(4097)))
                .isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());
        verifyNoInteractions(secretService);
    }

    @Test
    void failToUpdateWeb2SecretWhenBadAuthorization() {
        when(authorizationService.getChallengeForSetWeb2Secret(OWNER_ADDRESS, SECRET_NAME, SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, OWNER_ADDRESS))
                .thenReturn(false);
        assertThat(secretController.updateWeb2Secret(AUTHORIZATION, OWNER_ADDRESS, SECRET_NAME, SECRET_VALUE))
                .isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @Test
    void failToUpdateWeb2SecretWhenSecretIsMissing() throws NotAnExistingSecretException, SameSecretException {
        when(authorizationService.getChallengeForSetWeb2Secret(OWNER_ADDRESS, SECRET_NAME, SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, OWNER_ADDRESS))
                .thenReturn(true);
        doThrow(NotAnExistingSecretException.class).when(secretService)
                .updateSecret(SECRET_HEADER, SECRET_VALUE);
        assertThat(secretController.updateWeb2Secret(AUTHORIZATION, OWNER_ADDRESS, SECRET_NAME, SECRET_VALUE))
                .isEqualTo(ResponseEntity.notFound().build());
    }

    @Test
    void updateWeb2Secret() {
        when(authorizationService.getChallengeForSetWeb2Secret(OWNER_ADDRESS, SECRET_NAME, SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, OWNER_ADDRESS))
                .thenReturn(true);
        assertThat(secretController.updateWeb2Secret(AUTHORIZATION, OWNER_ADDRESS, SECRET_NAME, SECRET_VALUE))
                .isEqualTo(ResponseEntity.noContent().build());
    }
    //endregion

    // region Utils
    private String getRandomString(int size) {
        byte[] bytes = new byte[size];
        seed.nextBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    // endregion
}
