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

import com.iexec.common.contract.generated.Ownable;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.sms.secret.SecretUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@CrossOrigin
@RestController
public class AppComputeSecretController {
    private final AuthorizationService authorizationService;
    private final TeeTaskComputeSecretService teeTaskComputeSecretService;
    private final TeeTaskComputeSecretCountService teeTaskComputeSecretCountService;
    private final IexecHubService iexecHubService;

    private static final Map<String, String> invalidAuthorizationPayload = createErrorPayload("Invalid authorization");

    public AppComputeSecretController(AuthorizationService authorizationService,
                                      TeeTaskComputeSecretService teeTaskComputeSecretService,
                                      TeeTaskComputeSecretCountService teeTaskComputeSecretCountService,
                                      IexecHubService iexecHubService) {
        this.authorizationService = authorizationService;
        this.teeTaskComputeSecretService = teeTaskComputeSecretService;
        this.teeTaskComputeSecretCountService = teeTaskComputeSecretCountService;
        this.iexecHubService = iexecHubService;
    }

    // region App developer endpoints
    @PostMapping("/apps/{appAddress}/secrets/0")
    public ResponseEntity<Map<String, String>> addAppDeveloperAppComputeSecret(@RequestHeader("Authorization") String authorization,
                                                                               @PathVariable String appAddress,
//                                                                      @PathVariable long secretIndex,    // FIXME: enable once functioning has been validated
                                                                               @RequestBody String secretValue) {
        long secretIndex = 0;   // FIXME: remove once functioning has been validated.

        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(createErrorPayload("Secret size should not exceed 4 Kb"));
        }

        String challenge = authorizationService.getChallengeForSetAppComputeSecret(appAddress, secretIndex, secretValue);

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
    public ResponseEntity<Map<String, String>> isAppDeveloperAppComputeSecretPresent(@PathVariable String appAddress,
                                                                                     @PathVariable long secretIndex) {
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
    public ResponseEntity<Map<String, String>> setMaxRequesterSecretCountForAppCompute(
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
    // endregion

    // region App requester endpoint
    private static class RequesterAppComputeSecretData {
        private final String authorization;
        private final String requesterAddress;
        private final String appAddress;
        private final long secretIndex;
        private final String secretValue;

        public RequesterAppComputeSecretData(String authorization, String requesterAddress, String appAddress, long secretIndex, String secretValue) {
            this.authorization = authorization;
            this.requesterAddress = requesterAddress;
            this.appAddress = appAddress;
            this.secretIndex = secretIndex;
            this.secretValue = secretValue;
        }
    }
    @FunctionalInterface
    interface RequesterAppComputeSecretValidator {
        Optional<ResponseEntity<Map<String, String>>> validate(RequesterAppComputeSecretData data);
    }

    @PostMapping("/requesters/{requesterAddress}/apps/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<Map<String, String>> addRequesterAppComputeSecret(@RequestHeader("Authorization") String authorization,
                                                                            @PathVariable String requesterAddress,
                                                                            @PathVariable String appAddress,
                                                                            @PathVariable long secretIndex,
                                                                            @RequestBody String secretValue) {
        List<RequesterAppComputeSecretValidator> validationList = List.of(
                this::validateRequesterAppComputeSecretIndex,
                this::validateRequesterAppComputeSecretSize,
                this::validateRequesterAppComputeSecretAuthorization,
                this::validateRequesterAppComputeAppAddress,
                this::validateRequesterAppComputeSecretPresent,
                this::validateRequesterAppComputeSecretIndexAllowed
        );

        final RequesterAppComputeSecretData data = new RequesterAppComputeSecretData(
                authorization,
                requesterAddress,
                appAddress,
                secretIndex,
                secretValue
        );

        final Optional<ResponseEntity<Map<String, String>>> validationIssue = validationList.stream()
                .map(validator -> validator.validate(data))
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
        if (validationIssue.isPresent()) {
            return validationIssue.get();
        }

        teeTaskComputeSecretService.encryptAndSaveSecret(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretIndex,
                secretValue
        );
        return ResponseEntity.noContent().build();
    }

    private Optional<ResponseEntity<Map<String, String>>> validateRequesterAppComputeSecretIndex(RequesterAppComputeSecretData data) {
        // TODO: remove following bloc once functioning has been validated
        if (data.secretIndex > 0) {
            log.debug("Can't add more than a single app requester secret as of now." +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}]",
                    data.requesterAddress, data.appAddress, data.secretIndex);
            return Optional.of(
                    ResponseEntity
                            .badRequest()
                            .body(createErrorPayload("Can't add more than a single app requester secret as of now."))
            );
        }

        if (data.secretIndex < 0) {
            log.debug("Negative index are forbidden for app requester secrets." +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}]",
                    data.requesterAddress, data.appAddress, data.secretIndex);
            return Optional.of(
                    ResponseEntity
                            .badRequest()
                            .body(createErrorPayload("Negative index are forbidden for app requester secrets."))
            );
        }
        return Optional.empty();
    }

    private Optional<ResponseEntity<Map<String, String>>> validateRequesterAppComputeSecretSize(RequesterAppComputeSecretData data) {
        if (!SecretUtils.isSecretSizeValid(data.secretValue)) {
            return Optional.of(
                    ResponseEntity
                            .status(HttpStatus.PAYLOAD_TOO_LARGE)
                            .body(createErrorPayload("Secret size should not exceed 4 Kb"))
            );
        }
        return Optional.empty();
    }

    private Optional<ResponseEntity<Map<String, String>>> validateRequesterAppComputeSecretAuthorization(RequesterAppComputeSecretData data) {
        String challenge = authorizationService.getChallengeForSetRequesterAppComputeSecret(
                data.requesterAddress,
                data.appAddress,
                data.secretIndex,
                data.secretValue
        );

        if (!authorizationService.isSignedByHimself(challenge, data.authorization, data.requesterAddress)) {
            log.error("Unauthorized to addRequesterAppComputeSecret" +
                            " [requesterAddress:{}, appAddress:{}, expectedChallenge:{}]",
                    data.requesterAddress, data.appAddress, challenge);
            return Optional.of(
                    ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(invalidAuthorizationPayload)
            );
        }
        return Optional.empty();
    }

    private Optional<ResponseEntity<Map<String, String>>> validateRequesterAppComputeAppAddress(RequesterAppComputeSecretData data) {
        final Ownable appContract = iexecHubService.getOwnableContract(data.appAddress);
        if (appContract == null
                || BytesUtils.EMPTY_ADDRESS.equals(data.appAddress)
                || !Objects.equals(appContract.getContractAddress(), data.appAddress)) {
            log.debug("App does not exist" +
                            " [requesterAddress:{}, appAddress:{}]",
                    data.requesterAddress, data.appAddress);
            return Optional.of(ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(createErrorPayload("App does not exist"))) ;
        }
        return Optional.empty();
    }

    private Optional<ResponseEntity<Map<String, String>>> validateRequesterAppComputeSecretPresent(RequesterAppComputeSecretData data) {
        if (teeTaskComputeSecretService.isSecretPresent(
                OnChainObjectType.APPLICATION,
                data.appAddress,
                SecretOwnerRole.REQUESTER,
                data.requesterAddress,
                data.secretIndex)) {
            log.debug("Can't add requester secret as it already exists" +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}]",
                    data.requesterAddress, data.appAddress, data.secretIndex);
            return Optional.of(
                    ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(createErrorPayload("Secret already exists"))
            );
        }
        return Optional.empty();
    }

    private Optional<ResponseEntity<Map<String, String>>> validateRequesterAppComputeSecretIndexAllowed(RequesterAppComputeSecretData data) {
        final Optional<TeeTaskComputeSecretCount> oAllowedSecretsCount =
                teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(
                        data.appAddress,
                        SecretOwnerRole.REQUESTER
                );

        if (oAllowedSecretsCount.isEmpty()) {
            log.error("Can't add requester secret as no secret count has been provided" +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}]",
                    data.requesterAddress, data.appAddress, data.secretIndex);
            return Optional.of(
                    ResponseEntity
                            .badRequest()
                            .body(createErrorPayload("No secret count has been provided"))
            );
        }

        final Integer allowedSecretsCount = oAllowedSecretsCount.get().getSecretCount();
        if (data.secretIndex >= allowedSecretsCount) {
            log.error("Can't add requester secret as index is greater than allowed secrets count" +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}, secretCount:{}]",
                    data.requesterAddress, data.appAddress, data.secretIndex, allowedSecretsCount);
            return Optional.of(
                    ResponseEntity
                            .badRequest()
                            .body(createErrorPayload("Index is greater than allowed secrets count"))
            );
        }
        return Optional.empty();
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/requesters/{requesterAddress}/apps/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<Map<String, String>> isRequesterAppComputeSecretPresent(
            @PathVariable String requesterAddress,
            @PathVariable String appAddress,
            @PathVariable long secretIndex) {
        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretIndex
        );
        if (isSecretPresent) {
            log.debug("App requester secret found" +
                            " [requester: {}, appAddress: {}, secretIndex: {}]",
                    requesterAddress, appAddress, secretIndex);
            return ResponseEntity.noContent().build();
        }

        log.debug("App requester secret not found " +
                        " [requester: {}, appAddress: {}, secretIndex: {}]",
                requesterAddress, appAddress, secretIndex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorPayload("Secret not found"));
    }
    // endregion

    private static Map<String, String> createErrorPayload(String errorMessage) {
        return Map.of("error", errorMessage);
    }
}
