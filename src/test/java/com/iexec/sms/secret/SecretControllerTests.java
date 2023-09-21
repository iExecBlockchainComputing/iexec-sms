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

package com.iexec.sms.secret;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.web2.*;
import com.iexec.sms.secret.web3.Web3SecretService;
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

class SecretControllerTests {

    private static final String AUTHORIZATION = "AUTHORIZATION";
    private static final String CHALLENGE = "CHALLENGE";
    private static final String WEB2_OWNER_ADDRESS = "WEB2_OWNER_ADDRESS";
    private static final String WEB2_SECRET_NAME = "WEB2_SECRET_NAME";
    private static final String WEB2_SECRET_VALUE = "WEB2_SECRET_VALUE";
    private static final String WEB3_SECRET_ADDRESS = "WEB3_SECRET_ADDRESS";
    private static final String WEB3_SECRET_VALUE = "WEB3_SECRET_VALUE";

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private Web2SecretService web2SecretService;

    @Mock
    private Web3SecretService web3SecretService;

    @InjectMocks
    private SecretController secretController;

    private static final SecureRandom seed = new SecureRandom();

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    //region isWeb3SecretSet
    @Test
    void shouldReturnNoContentWhenWeb3SecretExists() {
        when(web3SecretService.isSecretPresent(WEB3_SECRET_ADDRESS))
                .thenReturn(true);
        assertThat(secretController.isWeb3SecretSet(WEB3_SECRET_ADDRESS))
                .isEqualTo(ResponseEntity.noContent().build());
        verifyNoInteractions(authorizationService, web2SecretService);
    }

    @Test
    void shouldReturnNotFoundWhenWeb3SecretDoesNotExist() {
        when(web3SecretService.isSecretPresent(WEB3_SECRET_ADDRESS))
                .thenReturn(false);
        assertThat(secretController.isWeb3SecretSet(WEB3_SECRET_ADDRESS))
                .isEqualTo(ResponseEntity.notFound().build());
        verifyNoInteractions(authorizationService, web2SecretService);
    }
    //endregion

    //region addWeb3Secret
    @Test
    void failToAddWeb3SecretWhenBadAuthorization() {
        when(authorizationService.getChallengeForSetWeb3Secret(WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, WEB3_SECRET_ADDRESS))
                .thenReturn(false);
        assertThat(secretController.addWeb3Secret(AUTHORIZATION, WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE))
                .isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        verifyNoInteractions(web2SecretService, web3SecretService);
    }

    @Test
    void failToAddWeb3SecretWhenPayloadTooLarge() {
        final String payloadTooLarge = getRandomString(4097);
        when(authorizationService.getChallengeForSetWeb3Secret(WEB3_SECRET_ADDRESS, payloadTooLarge))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, WEB3_SECRET_ADDRESS))
                .thenReturn(true);
        assertThat(secretController.addWeb3Secret(AUTHORIZATION, WEB3_SECRET_ADDRESS, payloadTooLarge))
                .isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());
        verifyNoInteractions(web2SecretService, web3SecretService);
    }

    @Test
    void failToAddWeb3SecretWhenSecretAlreadyExists() {
        when(authorizationService.getChallengeForSetWeb3Secret(WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, WEB3_SECRET_ADDRESS))
                .thenReturn(true);
        when(web3SecretService.addSecret(WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE))
                .thenReturn(false);
        assertThat(secretController.addWeb3Secret(AUTHORIZATION, WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE))
                .isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());
        verifyNoInteractions(web2SecretService);
        verify(web3SecretService).addSecret(WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE);
    }

    @Test
    void addWeb3Secret() {
        when(authorizationService.getChallengeForSetWeb3Secret(WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, WEB3_SECRET_ADDRESS))
                .thenReturn(true);
        when(web3SecretService.addSecret(WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE))
                .thenReturn(true);
        assertThat(secretController.addWeb3Secret(AUTHORIZATION, WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE))
                .isEqualTo(ResponseEntity.noContent().build());
        verifyNoInteractions(web2SecretService);
        verify(web3SecretService).addSecret(WEB3_SECRET_ADDRESS, WEB3_SECRET_VALUE);
    }
    //endregion

    //region isWeb2SecretSet
    @Test
    void shouldReturnNoContentWhenWeb2SecretExists() {
        when(web2SecretService.isSecretPresent(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME))
                .thenReturn(true);
        assertThat(secretController.isWeb2SecretSet(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME))
                .isEqualTo(ResponseEntity.noContent().build());
        verifyNoInteractions(authorizationService, web3SecretService);
    }

    @Test
    void shouldReturnNotFoundWhenWeb2SecretDoesNotExist() {
        when(web2SecretService.isSecretPresent(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME))
                .thenReturn(false);
        assertThat(secretController.isWeb2SecretSet(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME))
                .isEqualTo(ResponseEntity.notFound().build());
        verifyNoInteractions(authorizationService, web3SecretService);
    }
    //endregion

    //region addWeb2Secret
    @Test
    void failToAddWeb2SecretWhenBadAuthorization() {
        when(authorizationService.getChallengeForSetWeb2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WEB2_OWNER_ADDRESS))
                .thenReturn(false);
        assertThat(secretController.addWeb2Secret(AUTHORIZATION, WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        verifyNoInteractions(web2SecretService, web3SecretService);
    }

    @Test
    void failToAddWeb2SecretWhenPayloadTooLarge() {
        final String payloadTooLarge = getRandomString(4097);
        when(authorizationService.getChallengeForSetWeb2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, payloadTooLarge))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WEB2_OWNER_ADDRESS))
                .thenReturn(true);
        assertThat(secretController.addWeb2Secret(AUTHORIZATION, WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, payloadTooLarge))
                .isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());
        verifyNoInteractions(web2SecretService, web3SecretService);
    }

    @Test
    void failToAddWeb2SecretWhenSecretAlreadyExists() throws SecretAlreadyExistsException {
        when(authorizationService.getChallengeForSetWeb2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WEB2_OWNER_ADDRESS))
                .thenReturn(true);
        when(web2SecretService.addSecret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .thenThrow(new SecretAlreadyExistsException(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME));
        assertThat(secretController.addWeb2Secret(AUTHORIZATION, WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
            .isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());
        verify(web2SecretService).addSecret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE);
    }

    @Test
    void addWeb2Secret() throws SecretAlreadyExistsException {
        when(authorizationService.getChallengeForSetWeb2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WEB2_OWNER_ADDRESS))
                .thenReturn(true);
        when(web2SecretService.addSecret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .thenReturn(new Web2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE));
        assertThat(secretController.addWeb2Secret(AUTHORIZATION, WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .isEqualTo(ResponseEntity.noContent().build());
        verify(web2SecretService).addSecret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE);
    }
    //endregion

    //region updateWeb2Secret
    @Test
    void failToUpdateWeb2SecretWhenBadAuthorization() {
        when(authorizationService.getChallengeForSetWeb2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WEB2_OWNER_ADDRESS))
                .thenReturn(false);
        assertThat(secretController.updateWeb2Secret(AUTHORIZATION, WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        verifyNoInteractions(web2SecretService, web3SecretService);
    }

    @Test
    void failToUpdateWeb2SecretWhenPayloadTooLarge() {
        final String payloadTooLarge = getRandomString(4097);
        when(authorizationService.getChallengeForSetWeb2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, payloadTooLarge))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WEB2_OWNER_ADDRESS))
                .thenReturn(true);
        assertThat(secretController.updateWeb2Secret(AUTHORIZATION, WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, payloadTooLarge))
                .isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());
        verifyNoInteractions(web2SecretService, web3SecretService);
    }

    @Test
    void failToUpdateWeb2SecretWhenSecretIsMissing() throws NotAnExistingSecretException, SameSecretException {
        when(authorizationService.getChallengeForSetWeb2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WEB2_OWNER_ADDRESS))
                .thenReturn(true);
        doThrow(NotAnExistingSecretException.class).when(web2SecretService)
                .updateSecret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE);
        assertThat(secretController.updateWeb2Secret(AUTHORIZATION, WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .isEqualTo(ResponseEntity.notFound().build());
    }

    @Test
    void updateWeb2Secret() {
        when(authorizationService.getChallengeForSetWeb2Secret(WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WEB2_OWNER_ADDRESS))
                .thenReturn(true);
        assertThat(secretController.updateWeb2Secret(AUTHORIZATION, WEB2_OWNER_ADDRESS, WEB2_SECRET_NAME, WEB2_SECRET_VALUE))
                .isEqualTo(ResponseEntity.noContent().build());
    }
    //endregion

    String getRandomString(int size) {
        byte[] bytes = new byte[size];
        seed.nextBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
