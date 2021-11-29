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

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/apps")
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

    @PostMapping("/{appAddress}/secrets/0")
    public ResponseEntity<String> addAppRuntimeSecret(@RequestHeader("Authorization") String authorization,
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
                SecretOwnerRole.APPLICATION_DEVELOPER,
                null,
                secretIndex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        teeTaskRuntimeSecretService.encryptAndSaveSecret(
                DeployedObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                null,
                secretIndex,
                secretValue
        );
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<Void> isAppRuntimeSecretPresent(@PathVariable String appAddress,
                                                          @PathVariable long secretIndex) {
        final boolean isSecretPresent = teeTaskRuntimeSecretService.isSecretPresent(
                DeployedObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
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
                .getChallengeForSetAppRequesterRuntimeSecretCount(
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
                        SecretOwnerRole.REQUESTER
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

    private Map<String, String> createErrorPayload(String errorMessage) {
        return Map.of("error", errorMessage);
    }
}
