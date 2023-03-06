package com.iexec.sms.tee;

import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.common.tee.TeeFramework;
import com.iexec.common.web.ApiResponseBody;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.api.TeeSessionGenerationResponse;
import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.authorization.AuthorizationError;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.TeeSessionService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.web3j.crypto.Keys;

import java.util.Optional;
import java.util.stream.Stream;

import static com.iexec.sms.api.TeeSessionGenerationError.*;
import static com.iexec.sms.authorization.AuthorizationError.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TeeControllerTests {
    private final static String TASK_ID = "0x0";
    private final static String WORKER_ADDRESS = "0x1";
    private final static String ENCLAVE_CHALLENGE = "0x2";
    private final static String AUTHORIZATION = "0x2";
    private final static String CHALLENGE = "CHALLENGE";
    private final static String SESSION_ID = "SESSION_ID";
    private static final String SECRET_PROVISIONING_URL = "https://secretProvisioningUrl";
    private static final String LAS_IMAGE = "lasImage";


    @Mock
    AuthorizationService authorizationService;
    @Mock
    TeeChallengeService teeChallengeService;
    @Mock
    TeeSessionService teeSessionService;
    @Mock
    TeeServicesProperties teeServicesConfig;

    @Mock
    TeeAppProperties preComputeProperties;
    @Mock
    TeeAppProperties postComputeProperties;

    @InjectMocks
    TeeController teeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // region getTeeFramework
    @Test
    void shouldGetSconeFramework() {
        final TeeServicesProperties properties = new SconeServicesProperties(
                preComputeProperties,
                postComputeProperties,
                LAS_IMAGE
        );

        final TeeController teeController = new TeeController(
                authorizationService, teeChallengeService, teeSessionService, properties
        );

        final ResponseEntity<TeeFramework> response =
                teeController.getTeeFramework();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final TeeFramework result = response.getBody();
        assertEquals(TeeFramework.SCONE, result);
    }

    @Test
    void shouldGetGramineFramework() {
        final TeeServicesProperties properties = new GramineServicesProperties(
                preComputeProperties,
                postComputeProperties
        );

        final TeeController teeController = new TeeController(
                authorizationService, teeChallengeService, teeSessionService, properties
        );

        final ResponseEntity<TeeFramework> response =
                teeController.getTeeFramework();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final TeeFramework result = response.getBody();
        assertEquals(TeeFramework.GRAMINE, result);
    }
    // endregion

    // region getTeeServicesConfig
    @Test
    void shouldGetSconeProperties() {
        final TeeServicesProperties properties = new SconeServicesProperties(
                preComputeProperties,
                postComputeProperties,
                LAS_IMAGE
        );

        final TeeController teeController = new TeeController(
                authorizationService, teeChallengeService, teeSessionService, properties
        );

        final ResponseEntity<TeeServicesProperties> response =
                teeController.getTeeServicesProperties(TeeFramework.SCONE);

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
        final TeeServicesProperties properties = new GramineServicesProperties(
                preComputeProperties,
                postComputeProperties
        );

        final TeeController teeController = new TeeController(
                authorizationService, teeChallengeService, teeSessionService, properties
        );

        final ResponseEntity<TeeServicesProperties> response =
                teeController.getTeeServicesProperties(TeeFramework.GRAMINE);

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
        final TeeServicesProperties properties = new SconeServicesProperties(
                preComputeProperties,
                postComputeProperties,
                LAS_IMAGE
        );

        final TeeController teeController = new TeeController(
                authorizationService, teeChallengeService, teeSessionService, properties
        );

        final ResponseEntity<TeeServicesProperties> response =
                teeController.getTeeServicesProperties(TeeFramework.GRAMINE);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void shouldNotGetGraminePropertiesSinceSconeSms() {
        final TeeServicesProperties properties = new GramineServicesProperties(
                preComputeProperties,
                postComputeProperties
        );

        final TeeController teeController = new TeeController(
                authorizationService, teeChallengeService, teeSessionService, properties
        );

        final ResponseEntity<TeeServicesProperties> response =
                teeController.getTeeServicesProperties(TeeFramework.SCONE);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
    // endregion

    // region generateTeeSession
    @Test
    void shouldGenerateTeeSession() throws TeeSessionGenerationException {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        when(authorizationService.getChallengeForWorker(workerpoolAuthorization)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WORKER_ADDRESS)).thenReturn(true);
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true))
                .thenReturn(Optional.empty());
        when(teeSessionService.generateTeeSession(TASK_ID, Keys.toChecksumAddress(WORKER_ADDRESS), ENCLAVE_CHALLENGE))
                .thenReturn(new TeeSessionGenerationResponse(SESSION_ID, SECRET_PROVISIONING_URL));

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = teeController
                .generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals(SESSION_ID, response.getBody().getData().getSessionId());
        assertEquals(SECRET_PROVISIONING_URL, response.getBody().getData().getSecretProvisioningUrl());
        assertNull(response.getBody().getError());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceNotSignedByHimself() {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        when(authorizationService.getChallengeForWorker(workerpoolAuthorization)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WORKER_ADDRESS)).thenReturn(false);

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = teeController
                .generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
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
            TeeSessionGenerationError consequence) {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        when(authorizationService.getChallengeForWorker(workerpoolAuthorization)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WORKER_ADDRESS)).thenReturn(true);
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true))
                .thenReturn(Optional.of(cause));

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = teeController
                .generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNull(response.getBody().getData());
        assertNotEquals(null, response.getBody().getError());
        assertEquals(consequence, response.getBody().getError());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceEmptyResponse() throws TeeSessionGenerationException {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        when(authorizationService.getChallengeForWorker(workerpoolAuthorization)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WORKER_ADDRESS)).thenReturn(true);
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true))
                .thenReturn(Optional.empty());
        when(teeSessionService.generateTeeSession(TASK_ID, Keys.toChecksumAddress(WORKER_ADDRESS), ENCLAVE_CHALLENGE))
                .thenReturn(null);

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = teeController
                .generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
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
    void shouldNotGenerateTeeSessionSinceSessionIdGenerationFailed(Exception exception)
            throws TeeSessionGenerationException {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        when(authorizationService.getChallengeForWorker(workerpoolAuthorization)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WORKER_ADDRESS)).thenReturn(true);
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true))
                .thenReturn(Optional.empty());
        when(teeSessionService.generateTeeSession(TASK_ID, Keys.toChecksumAddress(WORKER_ADDRESS), ENCLAVE_CHALLENGE))
                .thenThrow(exception);

        final ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> response = teeController
                .generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNull(response.getBody().getData());
        assertNotEquals(null, response.getBody().getError());
        assertEquals(SECURE_SESSION_GENERATION_FAILED, response.getBody().getError());
    }
    // endregion
}