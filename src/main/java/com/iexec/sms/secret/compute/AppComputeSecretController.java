/*
 * Copyright 2021-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.web.ApiResponseBody;
import com.iexec.sms.api.SmsClient;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.SecretUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@CrossOrigin
@RestController
public class AppComputeSecretController {

    private final AuthorizationService authorizationService;
    private final TeeTaskComputeSecretService teeTaskComputeSecretService;

    private static final ApiResponseBody<String, List<String>> invalidAuthorizationPayload = createErrorPayload("Invalid authorization");

    static final String INVALID_SECRET_INDEX_FORMAT_MSG = "Secret index should be a positive number";
    static final String INVALID_SECRET_KEY_FORMAT_MSG = "Secret key should contain at most 64 characters from [0-9A-Za-z-_]";
    static final String SECRET_NOT_FOUND_MSG = "Secret not found";
    private static final Pattern secretKeyPattern = Pattern.compile("^[\\p{Alnum}-_]{"
            + TeeTaskComputeSecretHeader.SECRET_KEY_MIN_LENGTH + ","
            + TeeTaskComputeSecretHeader.SECRET_KEY_MAX_LENGTH + "}$");

    public AppComputeSecretController(AuthorizationService authorizationService,
                                      TeeTaskComputeSecretService teeTaskComputeSecretService) {
        this.authorizationService = authorizationService;
        this.teeTaskComputeSecretService = teeTaskComputeSecretService;
    }

    // region App developer endpoints

    /**
     * @deprecated Call /apps/{appAddress}/secrets endpoints in {@code AppComputeSecretController}
     */
    @Deprecated(forRemoval = true)
    @PostMapping("/apps/{appAddress}/secrets/1")
    public ResponseEntity<ApiResponseBody<String, List<String>>> addAppDeveloperAppComputeSecret(@RequestHeader("Authorization") String authorization,
                                                                                                 @PathVariable String appAddress, @RequestBody String secretValue) {
        return addApplicationDeveloperAppComputeSecret(authorization, appAddress, secretValue);
    }

    @PostMapping("/apps/{appAddress}/secrets")
    public ResponseEntity<ApiResponseBody<String, List<String>>> addApplicationDeveloperAppComputeSecret(@RequestHeader("Authorization") String authorization,
                                                                                                         @PathVariable String appAddress,
                                                                                                         @RequestBody String secretValue) {
        appAddress = appAddress.toLowerCase();
        String challenge = authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);

        if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
            log.error("Unauthorized to addAppDeveloperComputeComputeSecret [appAddress: {}, expectedChallenge: {}]",
                    appAddress, challenge);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(invalidAuthorizationPayload);
        }

        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(createErrorPayload("Secret size should not exceed 4 Kb"));
        }

        if (!teeTaskComputeSecretService.encryptAndSaveSecret(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                SmsClient.APP_DEVELOPER_SECRET_INDEX,
                secretValue
        )) {
            log.error("Can't add app developer secret as it already exists [appAddress:{}]",
                    appAddress);
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(createErrorPayload("Secret already exists"));
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * @deprecated Call /apps/{appAddress}/secrets endpoints in {@code AppComputeSecretController}
     */
    @Deprecated(forRemoval = true)
    @RequestMapping(method = RequestMethod.HEAD, path = "/apps/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<ApiResponseBody<String, List<String>>> isAppDeveloperAppComputeSecretPresent(@PathVariable String appAddress,
                                                                                                       @PathVariable String secretIndex) {
        //We're keeping this test anyway, because we still consider that a call to this old endpoint
        //with an invalid secretIndex should not go through, even if the secretIndex is no longer in the signature of the new method.
        try {
            checkSecretIndex(secretIndex);
        } catch (NumberFormatException e) {
            log.error(INVALID_SECRET_INDEX_FORMAT_MSG, e);
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(INVALID_SECRET_INDEX_FORMAT_MSG));
        }
        if (SmsClient.APP_DEVELOPER_SECRET_INDEX.equals(secretIndex)) {
            return isApplicationDeveloperAppComputeSecretPresent(appAddress);
        }
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorPayload(SECRET_NOT_FOUND_MSG));
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/apps/{appAddress}/secrets")
    public ResponseEntity<ApiResponseBody<String, List<String>>> isApplicationDeveloperAppComputeSecretPresent(@PathVariable String appAddress) {
        appAddress = appAddress.toLowerCase();

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                SmsClient.APP_DEVELOPER_SECRET_INDEX
        );
        if (isSecretPresent) {
            log.debug("App developer secret found [appAddress: {}]", appAddress);
            return ResponseEntity.noContent().build();
        }

        log.debug("App developer secret not found [appAddress: {}]", appAddress);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorPayload(SECRET_NOT_FOUND_MSG));
    }

    /**
     * Checks provided application developer index is in a valid range.
     * A valid index is a positive number.
     *
     * @param secretIndex Secret index value to check.
     */
    private void checkSecretIndex(String secretIndex) {
        int idx = Integer.parseInt(secretIndex);
        if (idx <= 0) {
            throw new NumberFormatException();
        }
    }
    // endregion

    // region App requester endpoint
    @PostMapping("/requesters/{requesterAddress}/secrets/{secretKey}")
    public ResponseEntity<ApiResponseBody<String, List<String>>> addRequesterAppComputeSecret(@RequestHeader("Authorization") String authorization,
                                                                                              @PathVariable String requesterAddress,
                                                                                              @PathVariable String secretKey,
                                                                                              @RequestBody String secretValue) {
        requesterAddress = requesterAddress.toLowerCase();

        String challenge = authorizationService.getChallengeForSetRequesterAppComputeSecret(
                requesterAddress,
                secretKey,
                secretValue
        );

        if (!authorizationService.isSignedByHimself(challenge, authorization, requesterAddress)) {
            log.error("Unauthorized to addRequesterAppComputeSecret [requesterAddress:{}, expectedChallenge:{}]",
                    requesterAddress, challenge);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(invalidAuthorizationPayload);
        }

        if (!secretKeyPattern.matcher(secretKey).matches()) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(INVALID_SECRET_KEY_FORMAT_MSG));
        }

        final List<String> badRequestErrors = validateRequesterAppComputeSecret(requesterAddress, secretKey, secretValue);
        if (!badRequestErrors.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload(badRequestErrors));
        }

        if (!teeTaskComputeSecretService.encryptAndSaveSecret(
                OnChainObjectType.APPLICATION,
                "",
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretKey,
                secretValue
        )) {
            log.debug("Can't add requester secret as it already exists" +
                            " [requesterAddress:{}, secretIndex:{}]",
                    requesterAddress, secretKey);
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(createErrorPayload("Secret already exists"));
        }
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
    public ResponseEntity<ApiResponseBody<String, List<String>>> isRequesterAppComputeSecretPresent(
            @PathVariable String requesterAddress,
            @PathVariable String secretKey) {
        requesterAddress = requesterAddress.toLowerCase();

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
                .body(createErrorPayload(SECRET_NOT_FOUND_MSG));
    }
    // endregion

    private static <T> ApiResponseBody<T, List<String>> createErrorPayload(String errorMessage) {
        return createErrorPayload(List.of(errorMessage));
    }

    private static <T> ApiResponseBody<T, List<String>> createErrorPayload(List<String> errors) {
        return ApiResponseBody
                .<T, List<String>>builder()
                .error(errors)
                .build();
    }
}
