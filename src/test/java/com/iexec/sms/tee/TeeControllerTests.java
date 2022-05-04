package com.iexec.sms.tee;

import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.common.web.ApiResponseBody;
import com.iexec.sms.api.ApiResponseBody;
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.authorization.AuthorizationError;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.TeeSessionGenerationException;
import com.iexec.sms.tee.session.TeeSessionService;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
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
import static com.iexec.sms.api.TeeSessionGenerationError.EXECUTION_NOT_AUTHORIZED_INVALID_SIGNATURE;
import static com.iexec.sms.authorization.AuthorizationError.*;
import static com.iexec.sms.authorization.AuthorizationError.GET_CHAIN_DEAL_FAILED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TeeControllerTests {
    private final static String TASK_ID = "0x0";
    private final static String WORKER_ADDRESS = "0x1";
    private final static String ENCLAVE_CHALLENGE = "0x2";
    private final static String AUTHORIZATION = "0x2";
    private final static String CHALLENGE = "CHALLENGE";
    private final static String SESSION_ID = "SESSION_ID";

    @Mock
    AuthorizationService authorizationService;
    @Mock
    TeeChallengeService teeChallengeService;
    @Mock
    TeeSessionService teeSessionService;
    @Mock
    TeeWorkflowConfiguration teeWorkflowConfig;

    @InjectMocks
    TeeController teeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true)).thenReturn(Optional.empty());
        when(teeSessionService.generateTeeSession(TASK_ID, Keys.toChecksumAddress(WORKER_ADDRESS), ENCLAVE_CHALLENGE)).thenReturn(SESSION_ID);

        final ResponseEntity<ApiResponseBody<String, TeeSessionGenerationError>> response =
                teeController.generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNotEquals(null, response.getBody().getData());
        assertNotEquals("", response.getBody().getData());
        assertNull(response.getBody().getErrors());
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

        final ResponseEntity<ApiResponseBody<String, TeeSessionGenerationError>> response =
                teeController.generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNull(response.getBody().getData());
        assertNotEquals(null, response.getBody().getErrors());
        assertEquals(TeeSessionGenerationError.REQUEST_NOT_SIGNED_BY_HIMSELF, response.getBody().getErrors());
    }

    private static Stream<Arguments> notAuthorizedParams() {
        return Stream.of(
                Arguments.of(EMPTY_PARAMS_UNAUTHORIZED, EXECUTION_NOT_AUTHORIZED_EMPTY_PARAMS_UNAUTHORIZED),
                Arguments.of(NO_MATCH_ONCHAIN_TYPE, EXECUTION_NOT_AUTHORIZED_NO_MATCH_ONCHAIN_TYPE),
                Arguments.of(GET_CHAIN_TASK_FAILED, EXECUTION_NOT_AUTHORIZED_GET_CHAIN_TASK_FAILED),
                Arguments.of(TASK_NOT_ACTIVE, EXECUTION_NOT_AUTHORIZED_TASK_NOT_ACTIVE),
                Arguments.of(GET_CHAIN_DEAL_FAILED, EXECUTION_NOT_AUTHORIZED_GET_CHAIN_DEAL_FAILED),
                Arguments.of(INVALID_SIGNATURE, EXECUTION_NOT_AUTHORIZED_INVALID_SIGNATURE)
        );
    }

    @ParameterizedTest
    @MethodSource("notAuthorizedParams")
    void shouldNotGenerateTeeSessionSinceNotAuthorized(AuthorizationError cause, TeeSessionGenerationError consequence) {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        when(authorizationService.getChallengeForWorker(workerpoolAuthorization)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WORKER_ADDRESS)).thenReturn(true);
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true)).thenReturn(Optional.of(cause));

        final ResponseEntity<ApiResponseBody<String, TeeSessionGenerationError>> response =
                teeController.generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNull(response.getBody().getData());
        assertNotEquals(null, response.getBody().getErrors());
        assertEquals(consequence, response.getBody().getErrors());
    }

    @Test
    void shouldNotGenerateTeeSessionSinceEmptySessionId() throws TeeSessionGenerationException {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        when(authorizationService.getChallengeForWorker(workerpoolAuthorization)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WORKER_ADDRESS)).thenReturn(true);
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true)).thenReturn(Optional.empty());
        when(teeSessionService.generateTeeSession(TASK_ID, Keys.toChecksumAddress(WORKER_ADDRESS), ENCLAVE_CHALLENGE)).thenReturn("");

        final ResponseEntity<ApiResponseBody<String, TeeSessionGenerationError>> response =
                teeController.generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    private static Stream<Arguments> exceptionOnSessionIdGeneration() {
        return Stream.of(
                Arguments.of(
                        new TeeSessionGenerationException(
                                SECURE_SESSION_GENERATION_FAILED,
                                String.format("Failed to generate secure session [taskId:%s, workerAddress:%s]", TASK_ID, WORKER_ADDRESS)
                        )
                ),
                Arguments.of(new RuntimeException())
        );
    }

    /**
     * {@link TeeController#generateTeeSession(String, WorkerpoolAuthorization)}
     * should catch every error thrown
     * by {@link TeeSessionService#generateTeeSession(String, String, String)}.
     */
    @ParameterizedTest
    @MethodSource("exceptionOnSessionIdGeneration")
    void shouldNotGenerateTeeSessionSinceSessionIdGenerationFailed(Exception exception) throws TeeSessionGenerationException {
        final WorkerpoolAuthorization workerpoolAuthorization = WorkerpoolAuthorization
                .builder()
                .chainTaskId(TASK_ID)
                .workerWallet(WORKER_ADDRESS)
                .enclaveChallenge(ENCLAVE_CHALLENGE)
                .build();

        when(authorizationService.getChallengeForWorker(workerpoolAuthorization)).thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, WORKER_ADDRESS)).thenReturn(true);
        when(authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true)).thenReturn(Optional.empty());
        when(teeSessionService.generateTeeSession(TASK_ID, Keys.toChecksumAddress(WORKER_ADDRESS), ENCLAVE_CHALLENGE)).thenThrow(exception);

        final ResponseEntity<ApiResponseBody<String, TeeSessionGenerationError>> response =
                teeController.generateTeeSession(AUTHORIZATION, workerpoolAuthorization);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotEquals(null, response.getBody());
        assertNull(response.getBody().getData());
        assertNotEquals(null, response.getBody().getErrors());
        assertEquals(SECURE_SESSION_GENERATION_FAILED, response.getBody().getErrors());
    }
    // endregion
}