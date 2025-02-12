/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee;

import com.iexec.common.web.ApiResponseBody;
import com.iexec.commons.poco.chain.WorkerpoolAuthorization;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.commons.poco.utils.SignatureUtils;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.api.TeeSessionGenerationResponse;
import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.authorization.AuthorizationError;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.TeeSessionService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static com.iexec.sms.authorization.AuthorizationError.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeeControllerTests {
    private static final String TASK_ID = "0x0";
    private static final String WORKER_ADDRESS = "0x1";
    private static final String ENCLAVE_CHALLENGE = "0x2";
    private static final String AUTHORIZATION = "0x2";
    private static final String SESSION_ID = "SESSION_ID";
    private static final String SECRET_PROVISIONING_URL = "https://secretProvisioningUrl";
    private static final String LAS_IMAGE = "lasImage";
    private static final String VERSION = "v5";

    @Mock
    AuthorizationService authorizationService;
    @Mock
    TeeChallengeService teeChallengeService;
    @Mock
    TeeSessionService teeSessionServiceScone;
    @Mock
    TeeSessionService teeSessionServiceGramine;

    final TeeAppProperties preComputeProperties = TeeAppProperties.builder()
            .image("pre-image")
            .fingerprint("pre-fingerprint")
            .heapSizeInBytes(3L)
            .entrypoint("pre-entrypoint")
            .build();
    final TeeAppProperties postComputeProperties = TeeAppProperties.builder()
            .image("post-image")
            .fingerprint("post-fingerprint")
            .heapSizeInBytes(5L)
            .entrypoint("post-entrypoint")
            .build();

    final TeeServicesProperties sconeProperties = new SconeServicesProperties(VERSION, preComputeProperties, postComputeProperties, LAS_IMAGE);
    final TeeServicesProperties gramineProperties = new GramineServicesProperties(VERSION, preComputeProperties, postComputeProperties);

    TeeController sconeTeeController;
    TeeController gramineTeeController;

    @BeforeEach
    void setUp() {
        when(teeSessionServiceScone.resolveTeeServiceProperties(any()))
                .thenAnswer(invocation -> {
                    final String version = invocation.getArgument(0, String.class);
                    if (version == null || VERSION.equals(version)) {
                        return sconeProperties;
                    }
                    throw new IllegalArgumentException("SMS is not configured to use required framework version");
                });
        sconeTeeController = new TeeController(authorizationService, teeChallengeService, teeSessionServiceScone);

        when(teeSessionServiceGramine.resolveTeeServiceProperties(any()))
                .thenAnswer(invocation -> {
                    final String version = invocation.getArgument(0, String.class);
                    if (version == null || VERSION.equals(version)) {
                        return gramineProperties;
                    }
                    throw new IllegalArgumentException("SMS is not configured to use required framework version");
                });
        gramineTeeController = new TeeController(authorizationService, teeChallengeService, teeSessionServiceGramine);
    }

    // region getTeeFramework
    @Test
    void shouldGetSconeFramework() {
        final ResponseEntity<TeeFramework> response =
                sconeTeeController.getTeeFramework();

        assertThat(response).isEqualTo(ResponseEntity.ok(TeeFramework.SCONE));
    }

    @Test
    void shouldGetGramineFramework() {
        final ResponseEntity<TeeFramework> response =
                gramineTeeController.getTeeFramework();

        assertThat(response).isEqualTo(ResponseEntity.ok(TeeFramework.GRAMINE));
    }
    // endregion

    // region getTeeServicesProperties
    @Test
    void shouldGetSconeProperties() {
        final ResponseEntity<TeeServicesProperties> response =
                sconeTeeController.getTeeServicesProperties(TeeFramework.SCONE);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final TeeServicesProperties result = response.getBody();
        assertNotNull(result);
        assertInstanceOf(SconeServicesProperties.class, result);
        assertEquals(TeeFramework.SCONE, result.getTeeFramework());
        assertEquals(preComputeProperties, result.getPreComputeProperties());
        assertEquals(postComputeProperties, result.getPostComputeProperties());
        assertEquals(postComputeProperties, result.getPostComputeProperties());
        assertEquals(LAS_IMAGE, ((SconeServicesProperties) result).getLasImage());
    }

    @Test
    void shouldGetGramineProperties() {
        final ResponseEntity<TeeServicesProperties> response =
                gramineTeeController.getTeeServicesProperties(TeeFramework.GRAMINE);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final TeeServicesProperties result = response.getBody();
        assertNotNull(result);
        assertInstanceOf(GramineServicesProperties.class, result);
        assertEquals(TeeFramework.GRAMINE, result.getTeeFramework());
        assertEquals(preComputeProperties, result.getPreComputeProperties());
        assertEquals(postComputeProperties, result.getPostComputeProperties());
    }

    @Test
    void shouldNotGetSconePropertiesSinceGramineSms() {
        final ResponseEntity<TeeServicesProperties> response =
                sconeTeeController.getTeeServicesProperties(TeeFramework.GRAMINE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldNotGetGraminePropertiesSinceSconeSms() {
        final ResponseEntity<TeeServicesProperties> response =
                gramineTeeController.getTeeServicesProperties(TeeFramework.SCONE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
    // endregion

    // region getTeeServicesPropertiesVersion
    @Test
    void shouldGetSconePropertiesVersion() {
        final ResponseEntity<TeeServicesProperties> response =
                sconeTeeController.getTeeServicesPropertiesVersion(TeeFramework.SCONE, VERSION);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final TeeServicesProperties result = response.getBody();
        assertNotNull(result);
        assertInstanceOf(SconeServicesProperties.class, result);
        assertEquals(TeeFramework.SCONE, result.getTeeFramework());
        assertEquals(preComputeProperties, result.getPreComputeProperties());
        assertEquals(postComputeProperties, result.getPostComputeProperties());
        assertEquals(LAS_IMAGE, ((SconeServicesProperties) result).getLasImage());
    }

    @Test
    void shouldGetGraminePropertiesVersion() {
        final ResponseEntity<TeeServicesProperties> response =
                gramineTeeController.getTeeServicesPropertiesVersion(TeeFramework.GRAMINE, VERSION);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final TeeServicesProperties result = response.getBody();
        assertNotNull(result);
        assertInstanceOf(GramineServicesProperties.class, result);
        assertEquals(TeeFramework.GRAMINE, result.getTeeFramework());
        assertEquals(preComputeProperties, result.getPreComputeProperties());
        assertEquals(postComputeProperties, result.getPostComputeProperties());
    }

    @Test
    void shouldNotGetSconePropertiesVersionSinceGramineSms() {
        final ResponseEntity<TeeServicesProperties> response =
                sconeTeeController.getTeeServicesPropertiesVersion(TeeFramework.GRAMINE, VERSION);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldNotGetSconePropertiesVersionSinceWrongVersion() {
        final ResponseEntity<TeeServicesProperties> response =
                sconeTeeController.getTeeServicesPropertiesVersion(TeeFramework.SCONE, "v6.0.4");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotGetGraminePropertiesVersionSinceSconeSms() {
        final ResponseEntity<TeeServicesProperties> response =
                gramineTeeController.getTeeServicesPropertiesVersion(TeeFramework.SCONE, VERSION);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldNotGetGraminePropertiesVersionSinceWrongVersion() {
        final ResponseEntity<TeeServicesProperties> response =
                gramineTeeController.getTeeServicesPropertiesVersion(TeeFramework.GRAMINE, "v6.0.4");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    // endregion

    // region generateTeeChallenge
    @ParameterizedTest
    @EnumSource(value = AuthorizationError.class)
    void shouldNotGenerateTeeChallengeWhenNotAuthorized(AuthorizationError cause) {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .enclaveChallenge("")
                .workerWallet("")
                .signature(new Signature(AUTHORIZATION))
                .build();
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization))
                .thenReturn(Optional.of(cause));
        final ResponseEntity<String> response = sconeTeeController.generateTeeChallenge(AUTHORIZATION, TASK_ID);
        assertThat(response).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        verifyNoInteractions(teeChallengeService);
    }

    @Test
    void shouldNotGenerateTeeChallengeWhenExceptionDuringGeneration() {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .enclaveChallenge("")
                .workerWallet("")
                .signature(new Signature(AUTHORIZATION))
                .build();
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization))
                .thenReturn(Optional.empty());
        when(teeChallengeService.getOrCreate(TASK_ID, false)).thenReturn(Optional.empty());
        final ResponseEntity<String> response = sconeTeeController.generateTeeChallenge(AUTHORIZATION, TASK_ID);
        assertThat(response).isEqualTo(ResponseEntity.notFound().build());
    }

    @Test
    void shouldGenerateTeeChallenge() throws GeneralSecurityException {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .enclaveChallenge("")
                .workerWallet("")
                .signature(new Signature(AUTHORIZATION))
                .build();
        final TeeChallenge teeChallenge = new TeeChallenge(TASK_ID, Instant.now().plusMillis(1000));
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization))
                .thenReturn(Optional.empty());
        when(teeChallengeService.getOrCreate(TASK_ID, false)).thenReturn(Optional.of(teeChallenge));
        final ResponseEntity<String> response = sconeTeeController.generateTeeChallenge(AUTHORIZATION, TASK_ID);
        assertThat(response).isEqualTo(ResponseEntity.ok(teeChallenge.getCredentials().getAddress()));
    }
    // endregion

    // region generateTeeSession
    @Test
    void shouldGenerateTeeSession() throws Exception {
        final Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        final String workerAddress = credentials.getAddress();
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(workerAddress)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        final String challenge = workerpoolAuthorization.getHash();
        final String authorization = SignatureUtils.signMessageHashAndGetSignature(challenge, credentials.getEcKeyPair())
                .getValue();
        when(authorizationService.isSignedByHimself(challenge, authorization, workerAddress)).thenCallRealMethod();
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization))
                .thenReturn(Optional.empty());
        when(teeSessionServiceScone.generateTeeSession(TASK_ID, Keys.toChecksumAddress(workerAddress), ENCLAVE_CHALLENGE))
                .thenReturn(new TeeSessionGenerationResponse(SESSION_ID, SECRET_PROVISIONING_URL));

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = sconeTeeController
                .generateTeeSession(authorization, workerpoolAuthorization);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals(SESSION_ID, response.getBody().getData().getSessionId());
        assertEquals(SECRET_PROVISIONING_URL, response.getBody().getData().getSecretProvisioningUrl());
        assertNull(response.getBody().getError());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceNotSignedByHimself() throws Exception {
        final Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        final String workerAddress = credentials.getAddress();
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(workerAddress)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        final String challenge = workerpoolAuthorization.getHash();
        final String authorization = SignatureUtils.signMessageHashAndGetSignature(challenge, Keys.createEcKeyPair())
                .getValue();
        when(authorizationService.isSignedByHimself(challenge, authorization, workerAddress)).thenCallRealMethod();

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = sconeTeeController
                .generateTeeSession(authorization, workerpoolAuthorization);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNull(response.getBody().getData());
        assertNotEquals(null, response.getBody().getError());
        assertEquals(TeeSessionGenerationError.INVALID_AUTHORIZATION, response.getBody().getError());
    }

    private static Stream<Arguments> notAuthorizedParams() {
        return Stream.of(
                Arguments.of(EMPTY_PARAMS_UNAUTHORIZED, EXECUTION_NOT_AUTHORIZED_EMPTY_PARAMS_UNAUTHORIZED),
                Arguments.of(NO_MATCH_ONCHAIN_TYPE, EXECUTION_NOT_AUTHORIZED_NO_MATCH_ONCHAIN_TYPE),
                Arguments.of(GET_CHAIN_TASK_FAILED, EXECUTION_NOT_AUTHORIZED_GET_CHAIN_TASK_FAILED),
                Arguments.of(TASK_NOT_ACTIVE, EXECUTION_NOT_AUTHORIZED_TASK_NOT_ACTIVE),
                Arguments.of(GET_CHAIN_DEAL_FAILED, EXECUTION_NOT_AUTHORIZED_GET_CHAIN_DEAL_FAILED),
                Arguments.of(INVALID_SIGNATURE, EXECUTION_NOT_AUTHORIZED_INVALID_SIGNATURE));
    }

    @ParameterizedTest
    @MethodSource("notAuthorizedParams")
    void shouldNotGenerateTeeSessionSinceNotAuthorized(AuthorizationError cause,
                                                       TeeSessionGenerationError consequence) throws Exception {
        final Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        final String workerAddress = credentials.getAddress();
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(workerAddress)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        final String challenge = workerpoolAuthorization.getHash();
        final String authorization = SignatureUtils.signMessageHashAndGetSignature(challenge, credentials.getEcKeyPair())
                .getValue();
        when(authorizationService.isSignedByHimself(challenge, authorization, workerAddress)).thenCallRealMethod();
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization))
                .thenReturn(Optional.of(cause));

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = sconeTeeController
                .generateTeeSession(authorization, workerpoolAuthorization);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNull(response.getBody().getData());
        assertNotEquals(null, response.getBody().getError());
        assertEquals(consequence, response.getBody().getError());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceEmptyResponse() throws Exception {
        final Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        final String workerAddress = credentials.getAddress();
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(workerAddress)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        final String challenge = workerpoolAuthorization.getHash();
        final String authorization = SignatureUtils.signMessageHashAndGetSignature(challenge, credentials.getEcKeyPair())
                .getValue();
        when(authorizationService.isSignedByHimself(challenge, authorization, workerAddress)).thenCallRealMethod();
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization))
                .thenReturn(Optional.empty());
        when(teeSessionServiceScone.generateTeeSession(TASK_ID, Keys.toChecksumAddress(workerAddress), ENCLAVE_CHALLENGE))
                .thenReturn(null);

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = sconeTeeController
                .generateTeeSession(authorization, workerpoolAuthorization);

        assertThat(response).isEqualTo(ResponseEntity.notFound().build());
    }

    private static Stream<Arguments> exceptionOnSessionIdGeneration() {
        return Stream.of(
                Arguments.of(
                        new TeeSessionGenerationException(
                                SECURE_SESSION_GENERATION_FAILED,
                                String.format("Failed to generate secure session [taskId:%s, workerAddress:%s]",
                                        TASK_ID, WORKER_ADDRESS))),
                Arguments.of(new RuntimeException()));
    }

    /**
     * {@link TeeController#generateTeeSession(String, WorkerpoolAuthorization)}
     * should catch every error thrown
     * by {@link TeeSessionService#generateTeeSession(String, String, String)}.
     */
    @ParameterizedTest
    @MethodSource("exceptionOnSessionIdGeneration")
    void shouldNotGenerateTeeSessionSinceSessionIdGenerationFailed(Exception exception) throws Exception {
        final Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        final String workerAddress = credentials.getAddress();
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(workerAddress)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        final String challenge = workerpoolAuthorization.getHash();
        final String authorization = SignatureUtils.signMessageHashAndGetSignature(challenge, credentials.getEcKeyPair())
                .getValue();
        when(authorizationService.isSignedByHimself(challenge, authorization, workerAddress)).thenCallRealMethod();
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization))
                .thenReturn(Optional.empty());
        when(teeSessionServiceScone.generateTeeSession(TASK_ID, Keys.toChecksumAddress(workerAddress), ENCLAVE_CHALLENGE))
                .thenThrow(exception);

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = sconeTeeController
                .generateTeeSession(authorization, workerpoolAuthorization);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNull(response.getBody().getData());
        assertNotEquals(null, response.getBody().getError());
        assertEquals(SECURE_SESSION_GENERATION_FAILED, response.getBody().getError());
    }
    // endregion
}
