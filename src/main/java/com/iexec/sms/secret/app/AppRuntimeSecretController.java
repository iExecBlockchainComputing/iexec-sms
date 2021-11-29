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

package com.iexec.sms.secret.app;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.SecretUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@CrossOrigin
@RestController
public class AppRuntimeSecretController {
    private final AuthorizationService authorizationService;
    private final TeeTaskRuntimeSecretService teeTaskRuntimeSecretService;
    private final TeeTaskRuntimeSecretCountService teeTaskRuntimeSecretCountService;

    public AppRuntimeSecretController(AuthorizationService authorizationService,
                                      TeeTaskRuntimeSecretService teeTaskRuntimeSecretService,
                                      TeeTaskRuntimeSecretCountService teeTaskRuntimeSecretCountService) {
        this.authorizationService = authorizationService;
        this.teeTaskRuntimeSecretService = teeTaskRuntimeSecretService;
        this.teeTaskRuntimeSecretCountService = teeTaskRuntimeSecretCountService;
    }

    // region App developer endpoints
    @PostMapping("/apps/{appAddress}/secrets/0")
    public ResponseEntity<String> addAppDeveloperAppRuntimeSecret(@RequestHeader("Authorization") String authorization,
                                                                  @PathVariable String appAddress,
//                                                                      @PathVariable long secretIndex,    // FIXME: enable once functioning has been validated
                                                                  @RequestBody String secretValue) {
        long secretIndex = 0;   // FIXME: remove once functioning has been validated.

        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        String challenge = authorizationService.getChallengeForSetAppRuntimeSecret(appAddress, secretIndex, secretValue);

        if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
            log.error("Unauthorized to addAppDeveloperAppRuntimeSecret" +
                            " [appAddress: {}, expectedChallenge: {}]",
                    appAddress, challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (teeTaskRuntimeSecretService.isSecretPresent(
                DeployedObjectType.APPLICATION,
                appAddress,
                OwnerRole.APPLICATION_DEVELOPER,
                null,
                secretIndex)) {
            log.error("Unauthorized to addAppDeveloperAppRuntimeSecret: " +
                            "secret already exists" +
                            " [appAddress:{}, secretIndex:{}]",
                    appAddress, secretIndex);
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        teeTaskRuntimeSecretService.encryptAndSaveSecret(
                DeployedObjectType.APPLICATION,
                appAddress,
                OwnerRole.APPLICATION_DEVELOPER,
                null,
                secretIndex,
                secretValue
        );
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/apps/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<Void> isAppDeveloperAppRuntimeSecretPresent(@PathVariable String appAddress,
                                                                      @PathVariable long secretIndex) {
        final boolean isSecretPresent = teeTaskRuntimeSecretService.isSecretPresent(
                DeployedObjectType.APPLICATION,
                appAddress,
                OwnerRole.APPLICATION_DEVELOPER,
                null,
                secretIndex
        );
        if (isSecretPresent) {
            log.info("App developer secret found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
            return ResponseEntity.noContent().build();
        }

        log.info("App developer secret not found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{appAddress}/requesters/secrets")
    public ResponseEntity<Map<String, String>> setRequesterSecretCountForApp(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String appAddress,
            @RequestBody Integer secretCount) {
        String challenge = authorizationService
                .getChallengeForSetAppRequesterAppRuntimeSecretCount(
                        appAddress,
                        secretCount
                );

        if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
            log.error("Unauthorized to setRequesterSecretCountForApp" +
                            " [appAddress: {}, expectedChallenge: {}]",
                    appAddress, challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final boolean isCountAlreadyPresent = teeTaskRuntimeSecretCountService
                .isAppRuntimeSecretCountPresent(
                        appAddress,
                        OwnerRole.REQUESTER
                );
        if (isCountAlreadyPresent) {
            log.info("Can't add app requester app secret count as it already exist"
            + " [appAddress:{}]", appAddress);
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret count already exists
        }

        if (secretCount == null) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorPayload("Secret count cannot be null."));
        }

        final boolean hasBeenInserted = teeTaskRuntimeSecretCountService.setAppRuntimeSecretCount(
                appAddress,
                OwnerRole.REQUESTER,
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
    @PostMapping("/requesters/{requesterAddress}/apps/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<String> addAppRequesterAppRuntimeSecret(@RequestHeader("Authorization") String authorization,
                                                                  @PathVariable String requesterAddress,
                                                                  @PathVariable String appAddress,
                                                                  @PathVariable long secretIndex,
                                                                  @RequestBody String secretValue) {
        // TODO: remove following bloc once functioning has been validated
        if (secretIndex > 0) {
            log.error("Can't add more than a single app requester secret as of now." +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}]",
                    requesterAddress, appAddress, secretIndex);
            return ResponseEntity.badRequest().build();
        }

        if (secretIndex < 0) {
            log.error("Negative index are forbidden for app requester secrets." +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}]",
                    requesterAddress, appAddress, secretIndex);
            return ResponseEntity.badRequest().build();
        }

        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        String challenge = authorizationService.getChallengeForSetAppRequesterAppRuntimeSecret(
                requesterAddress,
                appAddress,
                secretIndex,
                secretValue
        );

        if (!authorizationService.isSignedByHimself(challenge, authorization, requesterAddress)) {
            log.error("Unauthorized to addAppRequesterAppRuntimeSecret" +
                            " [requesterAddress:{}, appAddress:{}, expectedChallenge:{}]",
                    requesterAddress, appAddress, challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (teeTaskRuntimeSecretService.isSecretPresent(
                DeployedObjectType.APPLICATION,
                appAddress,
                OwnerRole.REQUESTER,
                requesterAddress,
                secretIndex)) {
            log.error("Unauthorized to addAppRequesterAppRuntimeSecret: " +
                            "secret already exists" +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}]",
                    requesterAddress, appAddress, secretIndex);
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        final Optional<TeeTaskRuntimeSecretCount> oAllowedSecretsCount =
                teeTaskRuntimeSecretCountService.getAppRuntimeSecretCount(
                        appAddress,
                        OwnerRole.REQUESTER
                );

        if (oAllowedSecretsCount.isEmpty()) {
            log.error("Unauthorized to addAppRequesterAppRuntimeSecret: " +
                            "no secret count has been provided" +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}]",
                    requesterAddress, appAddress, secretIndex);
            return ResponseEntity.badRequest().build();
        }

        final Integer allowedSecretsCount = oAllowedSecretsCount.get().getSecretCount();
        if (secretIndex >= allowedSecretsCount) {
            log.error("Unauthorized to addAppRequesterAppRuntimeSecret: " +
                            "secret index is greater than allowed secrets count" +
                            " [requesterAddress:{}, appAddress:{}, secretIndex:{}, secretCount:{}]",
                    requesterAddress, appAddress, secretIndex, allowedSecretsCount);
            return ResponseEntity.badRequest().build();
        }

        teeTaskRuntimeSecretService.encryptAndSaveSecret(
                DeployedObjectType.APPLICATION,
                appAddress,
                OwnerRole.REQUESTER,
                requesterAddress,
                secretIndex,
                secretValue
        );
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/requesters/{requesterAddress}/apps/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<Void> isAppRequesterAppRuntimeSecretPresent(
            @PathVariable String requesterAddress,
            @PathVariable String appAddress,
            @PathVariable long secretIndex) {
        final boolean isSecretPresent = teeTaskRuntimeSecretService.isSecretPresent(
                DeployedObjectType.APPLICATION,
                appAddress,
                OwnerRole.REQUESTER,
                requesterAddress,
                secretIndex
        );
        if (isSecretPresent) {
            log.info("App requester secret found" +
                            " [appRequester: {}, appAddress: {}, secretIndex: {}]",
                    requesterAddress, appAddress, secretIndex);
            return ResponseEntity.noContent().build();
        }

        log.info("App developer secret not found " +
                        " [appRequester: {}, appAddress: {}, secretIndex: {}]",
                requesterAddress, appAddress, secretIndex);
        return ResponseEntity.notFound().build();
    }
    // endregion

    private Map<String, String> createErrorPayload(String errorMessage) {
        return Map.of("error", errorMessage);
    }
}
