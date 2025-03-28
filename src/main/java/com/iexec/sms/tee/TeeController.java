/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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
import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.api.TeeSessionGenerationResponse;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.authorization.AuthorizationError;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.TeeSessionService;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Keys;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.iexec.sms.api.TeeSessionGenerationError.*;
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
    private final TeeServicesProperties teeServicesProperties;

    public TeeController(
            AuthorizationService authorizationService,
            TeeChallengeService teeChallengeService,
            TeeSessionService teeSessionService,
            @Value("${tee.worker.pipelines[0].version}") String version) {
        this.authorizationService = authorizationService;
        this.teeChallengeService = teeChallengeService;
        this.teeSessionService = teeSessionService;
        this.teeServicesProperties = teeSessionService.resolveTeeServiceProperties(version);
    }

    /**
     * Return which TEE framework this SMS is configured to use.
     *
     * @return TEE framework this SMS is configured to use.
     */
    @GetMapping("/framework")
    public ResponseEntity<TeeFramework> getTeeFramework() {
        return ResponseEntity.ok(teeServicesProperties.getTeeFramework());
    }

    /**
     * @return TEE services properties (pre-compute image uri, post-compute image uri, heap size, ...)
     * @deprecated Use {@link #getTeeServicesPropertiesVersion(TeeFramework, String)} instead.
     * This endpoint will be removed in future versions.
     *
     * <p>
     * Retrieve properties for TEE services. This includes properties
     * for pre-compute and post-compute stages
     * and potential TEE framework's specific data.
     * </p>
     */
    @Deprecated(since = "8.7.0", forRemoval = true)
    @GetMapping("/properties/{teeFramework}")
    public ResponseEntity<TeeServicesProperties> getTeeServicesProperties(
            @PathVariable TeeFramework teeFramework) {
        if (teeFramework != teeServicesProperties.getTeeFramework()) {
            log.error("SMS configured to use another TeeFramework " +
                    "[required:{}, actual:{}]", teeFramework, teeServicesProperties.getTeeFramework());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok(teeServicesProperties);
    }

    /**
     * Retrieve properties for TEE services related to the asked version. This includes properties
     * for pre-compute and post-compute stages
     * and potential TEE framework's specific data.
     *
     * @return TEE services properties (pre-compute image uri, post-compute image uri,
     * heap size, ...)
     */
    @GetMapping("/properties/{teeFramework}/{version}")
    public ResponseEntity<TeeServicesProperties> getTeeServicesPropertiesVersion(
            @PathVariable TeeFramework teeFramework,
            @PathVariable String version) {
        final TeeServicesProperties teeServicePropertiesVersion;
        try {
            teeServicePropertiesVersion = teeSessionService.resolveTeeServiceProperties(version);
        } catch (NoSuchElementException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (teeFramework != teeServicePropertiesVersion.getTeeFramework()) {
            log.error("SMS configured to use another TeeFramework [required:{}, actual:{}]",
                    teeFramework, teeServicePropertiesVersion.getTeeFramework());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.ok(teeServicePropertiesVersion);
    }

    /**
     * Generates an enclave challenge for a PoCo task.
     * <p>
     * This method is called by the scheduler.
     *
     * @param authorization Authorization to check the query legitimacy
     * @param chainTaskId   ID of the task the challenge will be produced for
     * @return The Ethereum address enclave challenge for the provided
     */
    @PostMapping("/challenges/{chainTaskId}")
    public ResponseEntity<String> generateTeeChallenge(@RequestHeader String authorization, @PathVariable String chainTaskId) {
        log.debug("generateTeeChallenge [authorization:{}, chainTaskId:{}]", authorization, chainTaskId);
        final Optional<AuthorizationError> authorizationError = authorizationService.isAuthorizedOnExecutionWithDetailedIssue(
                WorkerpoolAuthorization.builder()
                        .chainTaskId(chainTaskId)
                        .enclaveChallenge("")
                        .workerWallet("")
                        .signature(new Signature(authorization))
                        .build());
        if (authorizationError.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return teeChallengeService.getOrCreate(chainTaskId, false)
                .map(teeChallenge -> ResponseEntity.ok(teeChallenge.getCredentials().getAddress()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * The worker calls this endpoint to ask for a TEE session.
     * If the session is created the worker passes on the sessionId
     * to the enclave so the latter can talk to the CAS and get
     * the needed secrets.
     *
     * @return result
     * <ul>
     * <li>200 OK with the session id if success.
     * <li>404 NOT_FOUND if the task is not found.
     * <li>500 INTERNAL_SERVER_ERROR otherwise.
     * </ul>
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError>> generateTeeSession(
            @RequestHeader("Authorization") String authorization,
            @RequestBody WorkerpoolAuthorization workerpoolAuthorization) {
        String workerAddress = workerpoolAuthorization.getWorkerWallet();
        String challenge = workerpoolAuthorization.getHash();
        if (!authorizationService.isSignedByHimself(challenge, authorization, workerAddress)) {
            final ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError> body =
                    ApiResponseBody.<TeeSessionGenerationResponse, TeeSessionGenerationError>builder()
                            .error(INVALID_AUTHORIZATION)
                            .build();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(body);
        }
        final Optional<AuthorizationError> authorizationError =
                authorizationService.isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization);
        if (authorizationError.isPresent()) {
            final TeeSessionGenerationError teeSessionGenerationError =
                    authorizationToGenerationError.get(authorizationError.get());

            final ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError> body =
                    ApiResponseBody.<TeeSessionGenerationResponse, TeeSessionGenerationError>builder()
                            .error(teeSessionGenerationError)
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
            TeeSessionGenerationResponse teeSessionGenerationResponse = teeSessionService
                    .generateTeeSession(taskId, workerAddress, attestingEnclave);

            if (teeSessionGenerationResponse == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(ApiResponseBody.<TeeSessionGenerationResponse, TeeSessionGenerationError>builder()
                    .data(teeSessionGenerationResponse)
                    .build());
        } catch (TeeSessionGenerationException e) {
            log.error("Failed to generate secure session [taskId:{}, workerAddress:{}]",
                    taskId, workerAddress, e);
            final ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError> body =
                    ApiResponseBody.<TeeSessionGenerationResponse, TeeSessionGenerationError>builder()
                            .error(e.getError())
                            .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(body);
        } catch (Exception e) {
            log.error("Failed to generate secure session with unknown reason [taskId:{}, workerAddress:{}]",
                    taskId, workerAddress, e);
            final ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError> body =
                    ApiResponseBody.<TeeSessionGenerationResponse, TeeSessionGenerationError>builder()
                            .error(SECURE_SESSION_GENERATION_FAILED)
                            .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(body);
        }
    }
}
