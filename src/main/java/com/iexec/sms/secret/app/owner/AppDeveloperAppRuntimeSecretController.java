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
import com.iexec.sms.secret.app.requester.AppRequesterAppRuntimeSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/apps")
public class AppDeveloperAppRuntimeSecretController {
    private final AuthorizationService authorizationService;
    private final AppDeveloperAppRuntimeSecretService appDeveloperAppRuntimeSecretService;
    private final AppRequesterAppRuntimeSecretService appRequesterAppRuntimeSecretService;

    public AppDeveloperAppRuntimeSecretController(AuthorizationService authorizationService,
                                                  AppDeveloperAppRuntimeSecretService appDeveloperAppRuntimeSecretService,
                                                  AppRequesterAppRuntimeSecretService appRequesterAppRuntimeSecretService) {
        this.authorizationService = authorizationService;
        this.appDeveloperAppRuntimeSecretService = appDeveloperAppRuntimeSecretService;
        this.appRequesterAppRuntimeSecretService = appRequesterAppRuntimeSecretService;
    }

    @PostMapping("/{appAddress}/secrets/0")
    public ResponseEntity<String> addAppDeveloperAppRuntimeSecret(@RequestHeader("Authorization") String authorization,
                                                                  @PathVariable String appAddress,
//                                                              @PathVariable long secretIndex,    // FIXME: enable once functioning has been validated
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

        if (appDeveloperAppRuntimeSecretService.isSecretPresent(appAddress, secretIndex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        appDeveloperAppRuntimeSecretService.encryptAndSaveSecret(appAddress, secretIndex, secretValue);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<Void> isAppDeveloperAppRuntimeSecretPresent(@PathVariable String appAddress,
                                                                      @PathVariable long secretIndex) {
        if (appDeveloperAppRuntimeSecretService.isSecretPresent(appAddress, secretIndex)) {
            log.info("Secret found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
            return ResponseEntity.noContent().build();
        }

        log.info("Secret not found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{appAddress}/requesters/secrets")
    public ResponseEntity<String> setAppRequestersAppRuntimeSecretCount(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String appAddress,
            @RequestBody Integer secretCount) {
        String challenge = authorizationService
                .getChallengeForSetAppRequesterRuntimeSecretCount(
                        appAddress,
                        secretCount
                );

        if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
            log.error("Unauthorized to setAppRequestersAppRuntimeSecretCount" +
                            " [appAddress: {}, expectedChallenge: {}]",
                    appAddress, challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (appRequesterAppRuntimeSecretService.isAppRuntimeSecretCountPresent(appAddress)) {
            log.info("Can't add app requester app secret count as it already exist"
            + " [appAddress:{}]", appAddress);
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret count already exists
        }

        if (secretCount == null || secretCount < 0) {
            return ResponseEntity
                    .badRequest()
                    .body("Secret count should be positive. " +
                            "Can't accept value " + secretCount);
        }

        appRequesterAppRuntimeSecretService.setAppRuntimeSecretCount(
                appAddress,
                secretCount
        );
        return ResponseEntity.noContent().build();
    }
}
