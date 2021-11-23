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

package com.iexec.sms.secret.app.owner;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.SecretUtils;
import com.iexec.sms.secret.app.requester.AppRequesterRuntimeSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/apps")
public class AppDeveloperRuntimeSecretController {
    private final AuthorizationService authorizationService;
    private final AppDeveloperRuntimeSecretService appDeveloperRuntimeSecretService;
    private final AppRequesterRuntimeSecretService appRequesterRuntimeSecretService;

    public AppDeveloperRuntimeSecretController(AuthorizationService authorizationService,
                                               AppDeveloperRuntimeSecretService appDeveloperRuntimeSecretService,
                                               AppRequesterRuntimeSecretService appRequesterRuntimeSecretService) {
        this.authorizationService = authorizationService;
        this.appDeveloperRuntimeSecretService = appDeveloperRuntimeSecretService;
        this.appRequesterRuntimeSecretService = appRequesterRuntimeSecretService;
    }

    @PostMapping("/{appAddress}/secrets/0")
    public ResponseEntity<String> addApplicationRuntimeSecret(@RequestHeader("Authorization") String authorization,
                                                              @PathVariable String appAddress,
//                                                              @PathVariable long secretIndex,    // FIXME: enable once functioning has been validated
                                                              @RequestBody String secretValue) {
        long secretIndex = 0;   // FIXME: remove once functioning has been validated.

        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        String challenge = authorizationService.getChallengeForSetAppRuntimeSecret(appAddress, secretIndex, secretValue);

        if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
            log.error("Unauthorized to addRuntimeSecret [appAddress: {}, expectedChallenge: {}]",
                    appAddress, challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (appDeveloperRuntimeSecretService.isSecretPresent(appAddress, secretIndex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        appDeveloperRuntimeSecretService.encryptAndSaveSecret(appAddress, secretIndex, secretValue);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<Void> isApplicationRuntimeSecretPresent(@PathVariable String appAddress,
                                                                  @PathVariable long secretIndex) {
        if (appDeveloperRuntimeSecretService.isSecretPresent(appAddress, secretIndex)) {
            log.info("Secret found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
            return ResponseEntity.noContent().build();
        }

        log.info("Secret not found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{appAddress}/requesters/secrets")
    public ResponseEntity<String> setAppRequestersRuntimeSecretCount(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String appAddress,
            @RequestBody Integer secretCount) {
        String challenge = authorizationService
                .getChallengeForSetAppRequesterRuntimeSecretCount(appAddress);

        if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
            log.error("Unauthorized to setAppRequestersRuntimeSecretCount" +
                            " [appAddress: {}, expectedChallenge: {}]",
                    appAddress, challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (appRequesterRuntimeSecretService.isAppRuntimeSecretCountPresent(appAddress)) {
            log.info("Can't add app requester secret count as it already exist"
            + " [appAddress:{}]", appAddress);
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret count already exists
        }

        if (secretCount == null || secretCount < 0) {
            return ResponseEntity
                    .badRequest()
                    .body("Secret count should be positive. " +
                            "Can't accept value " + secretCount);
        }

        appRequesterRuntimeSecretService.setAppRuntimeSecretCount(
                appAddress,
                secretCount
        );
        return ResponseEntity.noContent().build();
    }
}
