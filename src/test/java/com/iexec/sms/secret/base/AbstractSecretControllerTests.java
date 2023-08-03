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
import com.iexec.sms.secret.exception.SecretAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static com.iexec.sms.secret.base.TestSecret.PLAIN_SECRET_VALUE;
import static com.iexec.sms.secret.base.TestSecret.TEST_SECRET;
import static com.iexec.sms.secret.base.TestSecretHeader.TEST_SECRET_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AbstractSecretControllerTests {
    private static final String AUTHORIZATION = "AUTHORIZATION";
    private static final String CHALLENGE = "CHALLENGE";

    private static final SecureRandom seed = new SecureRandom();

    @Mock
    private TestSecretService testSecretService;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    @Spy
    private TestSecretController testSecretController;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    //region isSecretSet
    @Test
    void shouldReturnNoContentWhenWeb3SecretExists() {
        when(testSecretService.isSecretPresent(TEST_SECRET_HEADER))
                .thenReturn(true);
        assertThat(testSecretController.isSecretSet(TEST_SECRET_HEADER))
                .isEqualTo(ResponseEntity.noContent().build());
        verify(testSecretService).isSecretPresent(TEST_SECRET_HEADER);
    }

    @Test
    void shouldReturnNotFoundWhenWeb3SecretDoesNotExist() {
        when(testSecretService.isSecretPresent(TEST_SECRET_HEADER))
                .thenReturn(false);
        assertThat(testSecretController.isSecretSet(TEST_SECRET_HEADER))
                .isEqualTo(ResponseEntity.notFound().build());
        verify(testSecretService).isSecretPresent(TEST_SECRET_HEADER);
    }
    //endregion

    //region addSecret
    @Test
    void failToAddSecretWhenPayloadTooLarge() {
        assertThat(testSecretController.addSecret(AUTHORIZATION, TEST_SECRET_HEADER, getRandomString(4097)))
                .isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());
        verifyNoInteractions(testSecretService);
    }

    @Test
    void failToAddWeb3SecretWhenBadAuthorization() {
        when(authorizationService.getChallengeForSetWeb3Secret(TEST_SECRET_HEADER.getId(), PLAIN_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, TEST_SECRET_HEADER.getId()))
                .thenReturn(false);
        assertThat(testSecretController.addSecret(AUTHORIZATION, TEST_SECRET_HEADER, PLAIN_SECRET_VALUE))
                .isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @Test
    void failToAddWeb3SecretWhenSecretAlreadyExists() throws SecretAlreadyExistsException {
        when(authorizationService.getChallengeForSetWeb3Secret(TEST_SECRET_HEADER.getId(), PLAIN_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, TEST_SECRET_HEADER.getId()))
                .thenReturn(true);
        when(testSecretService.addSecret(TEST_SECRET_HEADER, PLAIN_SECRET_VALUE))
                .thenThrow(new SecretAlreadyExistsException(TEST_SECRET_HEADER));
        assertThat(testSecretController.addSecret(AUTHORIZATION, TEST_SECRET_HEADER, PLAIN_SECRET_VALUE))
                .isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());
        verify(testSecretService).addSecret(TEST_SECRET_HEADER, PLAIN_SECRET_VALUE);
    }

    @Test
    void addWeb3Secret() throws SecretAlreadyExistsException {
        when(authorizationService.getChallengeForSetWeb3Secret(TEST_SECRET_HEADER.getId(), PLAIN_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, TEST_SECRET_HEADER.getId()))
                .thenReturn(true);
        when(testSecretService.addSecret(TEST_SECRET_HEADER, PLAIN_SECRET_VALUE))
                .thenReturn(TEST_SECRET);
        assertThat(testSecretController.addSecret(AUTHORIZATION, TEST_SECRET_HEADER, PLAIN_SECRET_VALUE))
                .isEqualTo(ResponseEntity.noContent().build());
        verify(testSecretService).addSecret(TEST_SECRET_HEADER, PLAIN_SECRET_VALUE);
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