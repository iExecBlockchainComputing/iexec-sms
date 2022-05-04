/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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


import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.common.tee.TeeSessionGenerationError;
import com.iexec.common.tee.TeeWorkflowSharedConfiguration;
import com.iexec.common.web.ApiResponseBody;
import com.iexec.sms.authorization.AuthorizationError;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.TeeSessionService;
import com.iexec.sms.tee.workflow.TeeWorkflowConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Keys;

import java.util.Map;
import java.util.Optional;

import static com.iexec.common.tee.TeeSessionGenerationError.*;
import static com.iexec.sms.authorization.AuthorizationError.*;

@Slf4j
@RestController
@RequestMapping("/tee")
public class TeeController {
    private static final Map<AuthorizationError, TeeSessionGenerationError> authorizationToGenerationError =
            Map.of(
                    EMPTY_PARAMS_UNAUTHORIZED, EXECUTION_NOT_AUTHORIZED_EMPTY_PARAMS_UNAUTHORIZED,
                    NO_MATCH_ONCHAIN_TYPE, EXECUTION_NOT_AUTHORIZED_NO_MATCH_ONCHAIN_TYPE,
                    GET_CHAIN_TASK_FAILED, EXECUTION_NOT_AUTHORIZED_GET_CHAIN_TASK_FAILED,
                    TASK_NOT_ACTIVE, EXECUTION_NOT_AUTHORIZED_TASK_NOT_ACTIVE,
                    GET_CHAIN_DEAL_FAILED, EXECUTION_NOT_AUTHORIZED_GET_CHAIN_DEAL_FAILED,
                    INVALID_SIGNATURE, EXECUTION_NOT_AUTHORIZED_INVALID_SIGNATURE
            );

    private final AuthorizationService authorizationService;
    private final TeeChallengeService teeChallengeService;
    private final TeeSessionService teeSessionService;
    private final TeeWorkflowConfiguration teeWorkflowConfig;

    public TeeController(
            AuthorizationService authorizationService,
            TeeChallengeService teeChallengeService,
            TeeSessionService teeSessionService,
            TeeWorkflowConfiguration teeWorkflowConfig) {
        this.authorizationService = authorizationService;
        this.teeChallengeService = teeChallengeService;
        this.teeSessionService = teeSessionService;
        this.teeWorkflowConfig = teeWorkflowConfig;
    }

    /**
     * Retrieve configuration for tee workflow. This includes configuration
     * for pre-compute and post-compute stages.
     * <p>
     * Note: Being able to read the fingerprints on this endpoint is not required
     * for the workflow but it might be convenient to keep it for
     * transparency purposes.
     *
     * @return tee workflow config (pre-compute image uri, post-compute image uri,
     * pre-compute fingerprint, heap size, ...)
     */
    @GetMapping("/workflow/config")
    public ResponseEntity<TeeWorkflowSharedConfiguration> getTeeWorkflowSharedConfig() {
        return ResponseEntity.ok(teeWorkflowConfig.getSharedConfiguration());
    }

    /**
     * Called by the core, not the worker
     */
    @PostMapping("/challenges/{chainTaskId}")
    public ResponseEntity<String> generateTeeChallenge(@PathVariable String chainTaskId) {
        Optional<TeeChallenge> executionChallenge =
                teeChallengeService.getOrCreate(chainTaskId, false);
        return executionChallenge
                .map(teeChallenge -> ResponseEntity
                        .ok(teeChallenge.getCredentials().getAddress()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * The worker calls this endpoint to ask for a TEE session.
     * If the session is created the worker passes on the sessionId
     * to the enclave so the latter can talk to the CAS and get
     * the needed secrets.
     *
     * @return
     *      200 OK with the session id if success,
     *      404 NOT_FOUND if the task is not found,
     *      500 INTERNAL_SERVER_ERROR otherwise.
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponseBody<String, TeeSessionGenerationError>> generateTeeSession(
            @RequestHeader("Authorization") String authorization,
            @RequestBody WorkerpoolAuthorization workerpoolAuthorization) {
        String workerAddress = workerpoolAuthorization.getWorkerWallet();
        String challenge = authorizationService.getChallengeForWorker(workerpoolAuthorization);
        if (!authorizationService.isSignedByHimself(challenge, authorization, workerAddress)) {
            final ApiResponseBody<String, TeeSessionGenerationError> body =
                    ApiResponseBody.<String, TeeSessionGenerationError>builder()
                            .errors(REQUEST_NOT_SIGNED_BY_HIMSELF)
                            .build();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(body);
        }
        final Optional<AuthorizationError> authorizationError =
                authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, true);
        if (authorizationError.isPresent()) {
            final TeeSessionGenerationError teeSessionGenerationError =
                    authorizationToGenerationError.get(authorizationError.get());

            final ApiResponseBody<String, TeeSessionGenerationError> body =
                    ApiResponseBody.<String, TeeSessionGenerationError>builder()
                            .errors(teeSessionGenerationError)
                            .build();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(body);
        }
        String taskId = workerpoolAuthorization.getChainTaskId();
        workerAddress = Keys.toChecksumAddress(workerAddress);
        String attestingEnclave = workerpoolAuthorization.getEnclaveChallenge();
        log.info("TEE session request [taskId:{}, workerAddress:{}]",
                taskId, workerAddress);
        try {
            String sessionId = teeSessionService
                    .generateTeeSession(taskId, workerAddress, attestingEnclave);

            if (sessionId.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(ApiResponseBody.<String, TeeSessionGenerationError>builder().data(sessionId).build());
        } catch(Exception e) {
            log.error("Failed to generate secure session [taskId:{}, workerAddress:{}]",
                    taskId, workerAddress, e);
            final ApiResponseBody<String, TeeSessionGenerationError> body =
                    ApiResponseBody.<String, TeeSessionGenerationError>builder()
                            .errors(SECURE_SESSION_GENERATION_FAILED)
                            .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(body);
        }
    }
}
