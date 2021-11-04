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

import java.nio.charset.StandardCharsets;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/apps")
public class ApplicationRuntimeSecretController {
    private final AuthorizationService authorizationService;
    private final ApplicationRuntimeSecretService applicationRuntimeSecretService;

    public ApplicationRuntimeSecretController(AuthorizationService authorizationService,
                                              ApplicationRuntimeSecretService applicationRuntimeSecretService) {
        this.authorizationService = authorizationService;
        this.applicationRuntimeSecretService = applicationRuntimeSecretService;
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

        if (applicationRuntimeSecretService.isSecretPresent(appAddress, secretIndex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        applicationRuntimeSecretService.encryptAndSaveSecret(appAddress, secretIndex, secretValue);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/{appAddress}/secrets/{secretIndex}")
    public ResponseEntity<Void> isApplicationRuntimeSecretPresent(@PathVariable String appAddress,
                                                                  @PathVariable long secretIndex) {
        if (applicationRuntimeSecretService.isSecretPresent(appAddress, secretIndex)) {
            log.info("Secret found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
            return ResponseEntity.noContent().build();
        }

        log.info("Secret not found [appAddress: {}, secretIndex: {}]", appAddress, secretIndex);
        return ResponseEntity.notFound().build();
    }
}
