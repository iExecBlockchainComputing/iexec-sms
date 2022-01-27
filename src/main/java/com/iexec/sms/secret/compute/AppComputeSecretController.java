/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.compute;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.SecretUtils;
import com.iexec.common.web.ApiResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@CrossOrigin
@RestController
public class AppComputeSecretController {
    private final AuthorizationService authorizationService;
    private final TeeTaskComputeSecretService teeTaskComputeSecretService;
    private final TeeTaskComputeSecretCountService teeTaskComputeSecretCountService;

    private static final ApiResponseBody<String> invalidAuthorizationPayload = createErrorPayload("Invalid authorization");

    static final String INVALID_SECRET_INDEX_FORMAT_MSG = "Secret index should be a positive number";
    static final String INVALID_SECRET_KEY_FORMAT_MSG = "Secret key should contain at most 64 characters from [0-9A-Za-z-_]";

    private static final Pattern secretKeyPattern = Pattern.compile("^[\\p{Alnum}-_]{"
            + TeeTaskComputeSecret.SECRET_KEY_MIN_LENGTH + ","
            + TeeTaskComputeSecret.SECRET_KEY_MAX_LENGTH + "}$");

    public AppComputeSecretController(AuthorizationService authorizationService,
                                      TeeTaskComputeSecretService teeTaskComputeSecretService,
                                      TeeTaskComputeSecretCountService teeTaskComputeSecretCountService) {
        this.authorizationService = authorizationService;
        this.teeTaskComputeSecretService = teeTaskComputeSecretService;
        this.teeTaskComputeSecretCountService = teeTaskComputeSecretCountService;
    }

    // region App developer endpoints
    @PostMapping("/apps/{appAddress}/secrets/0")
    public ResponseEntity<ApiResponseBody<String>> addAppDeveloperAppComputeSecret(@RequestHeader("Authorization") String authorization,
                                                                               @PathVariable String appAddress,
//                                                                      @PathVariable String secretIndex,    // FIXME: enable once functioning has been validated
                                                                               @RequestBody String secretValue) {
        String secretIndex = "0";   // FIXME: remove once functioning has been validated.

        try {
            int idx = Integer.parseInt(secretIndex);
            if (idx < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            log.error(INVALID_SECRET_INDEX_FORMAT_MSG, e);
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(INVALID_SECRET_INDEX_FORMAT_MSG));
        }

        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(createErrorPayload("Secret size should not exceed 4 Kb"));
        }

        String challenge = authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, secretIndex, secretValue);

        if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
            log.error("Unauthorized to addAppDeveloperComputeComputeSecret" +
                            " [appAddress: {}, expectedChallenge: {}]",
                    appAddress, challenge);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(invalidAuthorizationPayload);
        }

        if (teeTaskComputeSecretService.isSecretPresent(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                secretIndex)) {
            log.error("Can't add app developer secret as it already exists" +
                            " [appAddress:{}, secretIndex:{}]",
                    appAddress, secretIndex);
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(createErrorPayload("Secret already exists"));
        }

        teeTaskComputeSecretService.encryptAndSaveSecret(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                secretIndex,
                secretValue
        );
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/apps/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<ApiResponseBody<String>> isAppDeveloperAppComputeSecretPresent(@PathVariable String appAddress,
                                                                                         @PathVariable String secretIndex) {
        try {
            int idx = Integer.parseInt(secretIndex);
            if (idx < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            log.error(INVALID_SECRET_INDEX_FORMAT_MSG, e);
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(INVALID_SECRET_INDEX_FORMAT_MSG));
        }

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                secretIndex
        );
        if (isSecretPresent) {
            log.debug("App developer secret found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
            return ResponseEntity.noContent().build();
        }

        log.debug("App developer secret not found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorPayload("Secret not found"));
    }

    @PostMapping("/apps/{appAddress}/requesters/secrets-count")
    public ResponseEntity<ApiResponseBody<String>> setMaxRequesterSecretCountForAppCompute(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String appAddress,
            @RequestBody int secretCount) {
        if (secretCount < 0) {
            log.debug("Can't add app requester app secret count as it should not be negative"
                            + " [appAddress:{}, secretCount:{}]",
                    appAddress, secretCount);
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(
                            "Secret count should be positive. " +
                                    "Can't accept value " + secretCount
                    ));
        }

        String challenge = authorizationService
                .getChallengeForSetRequesterAppComputeSecretCount(
                        appAddress,
                        secretCount
                );

        if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
            log.error("Unauthorized to setRequesterSecretCountForAppCompute" +
                            " [appAddress: {}, expectedChallenge: {}]",
                    appAddress, challenge);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(invalidAuthorizationPayload);
        }

        final boolean isCountAlreadyPresent = teeTaskComputeSecretCountService
                .isMaxAppComputeSecretCountPresent(
                        appAddress,
                        SecretOwnerRole.REQUESTER
                );
        if (isCountAlreadyPresent) {
            log.debug("Can't add app requester app secret count as it already exist"
                    + " [appAddress:{}]", appAddress);
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(createErrorPayload("Secret count already exists"));
        }

        final boolean hasBeenInserted = teeTaskComputeSecretCountService.setMaxAppComputeSecretCount(
                appAddress,
                SecretOwnerRole.REQUESTER,
                secretCount
        );

        if (!hasBeenInserted) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(
                            "Secret count should be positive. " +
                            "Can't accept value " + secretCount
                    ));
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/apps/{appAddress}/requesters/secrets-count")
    public ResponseEntity<ApiResponseBody<Integer>> getMaxRequesterSecretCountForAppCompute(@PathVariable String appAddress) {
        final Optional<TeeTaskComputeSecretCount> secretCount =
                teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(appAddress, SecretOwnerRole.REQUESTER);
        if (secretCount.isPresent()) {
            log.debug("Requester secret count found [appAddress: {}]", appAddress);
            return ResponseEntity.ok(
                    ApiResponseBody
                            .<Integer>builder()
                            .data(secretCount.get().getSecretCount())
                            .build()
            );
        }

        log.debug("Requester secret count not found [appAddress: {}]", appAddress);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorPayload("Secret count not found"));
    }
    // endregion

    // region App requester endpoint
    @PostMapping("/requesters/{requesterAddress}/secrets/{secretKey}")
    public ResponseEntity<ApiResponseBody<String>> addRequesterAppComputeSecret(@RequestHeader("Authorization") String authorization,
                                                                            @PathVariable String requesterAddress,
                                                                            @PathVariable String secretKey,
                                                                            @RequestBody String secretValue) {
        if (!secretKeyPattern.matcher(secretKey).matches()) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(INVALID_SECRET_KEY_FORMAT_MSG));
        }

        String challenge = authorizationService.getChallengeForSetRequesterAppComputeSecret(
                requesterAddress,
                secretKey,
                secretValue
        );

        if (!authorizationService.isSignedByHimself(challenge, authorization, requesterAddress)) {
            log.error("Unauthorized to addRequesterAppComputeSecret" +
                            " [requesterAddress:{}, expectedChallenge:{}]",
                    requesterAddress, challenge);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(invalidAuthorizationPayload);
        }

        final List<String> badRequestErrors = validateRequesterAppComputeSecret(requesterAddress, secretKey, secretValue);
        if (!badRequestErrors.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(badRequestErrors));
        }

        if (teeTaskComputeSecretService.isSecretPresent(
                OnChainObjectType.APPLICATION,
                "",
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretKey)) {
            log.debug("Can't add requester secret as it already exists" +
                            " [requesterAddress:{}, secretIndex:{}]",
                    requesterAddress, secretKey);
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(createErrorPayload("Secret already exists"));
        }

        teeTaskComputeSecretService.encryptAndSaveSecret(
                OnChainObjectType.APPLICATION,
                "",
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretKey,
                secretValue
        );
        return ResponseEntity.noContent().build();
    }

    private List<String> validateRequesterAppComputeSecret(
            String requesterAddress,
            String secretKey,
            String secretValue) {
        List<String> errors = new ArrayList<>();

        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            final String errorMessage = "Secret size should not exceed 4 Kb";
            log.debug("{} [requesterAddress:{}, secretIndex:{}, secretLength:{}]",
                    errorMessage, requesterAddress, secretKey, secretValue.length()
            );
            errors.add(errorMessage);
        }

        return errors;
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/requesters/{requesterAddress}/secrets/{secretKey}")
    public ResponseEntity<ApiResponseBody<String>> isRequesterAppComputeSecretPresent(
            @PathVariable String requesterAddress,
            @PathVariable String secretKey) {
        if (!secretKeyPattern.matcher(secretKey).matches()) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(INVALID_SECRET_KEY_FORMAT_MSG));
        }

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(
                OnChainObjectType.APPLICATION,
                "",
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretKey
        );

        String messageDetails = MessageFormat.format("[requester: {0}, secretIndex: {1}]",
                requesterAddress, secretKey);
        if (isSecretPresent) {
            log.debug("App requester secret found {}", messageDetails);
            return ResponseEntity.noContent().build();
        }

        log.debug("App requester secret not found {}", messageDetails);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorPayload("Secret not found"));
    }
    // endregion

    private static <T> ApiResponseBody<T> createErrorPayload(String errorMessage) {
        return createErrorPayload(List.of(errorMessage));
    }

    private static <T> ApiResponseBody<T> createErrorPayload(List<String> errors) {
        return ApiResponseBody
                .<T>builder()
                .errors(errors)
                .build();
    }
}
