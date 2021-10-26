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

package com.iexec.sms.secret;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.runtime.RuntimeSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/apps")
public class RuntimeSecretController {
    private final AuthorizationService authorizationService;
    private final RuntimeSecretService runtimeSecretService;
    private final SecretUtils secretUtils;

    public RuntimeSecretController(AuthorizationService authorizationService, RuntimeSecretService runtimeSecretService, SecretUtils secretUtils) {
        this.authorizationService = authorizationService;
        this.runtimeSecretService = runtimeSecretService;
        this.secretUtils = secretUtils;
    }

    @PostMapping("/{appAddress}/secrets/0}")
    public ResponseEntity<String> addRuntimeSecret(@RequestHeader("Authorization") String authorization,
                                                   @PathVariable String appAddress,
//                                                   @PathVariable long secretIndex,    // FIXME: enable once functioning has been validated
                                                   @RequestBody String secretValue) {
        long secretIndex = 0;   // FIXME: remove once functioning has been validated.
        if (secretUtils.isInProduction(authorization)) {
            String challenge = authorizationService.getChallengeForSetRuntimeSecret(appAddress, secretIndex, secretValue);

            if (!authorizationService.isSignedByOwner(challenge, authorization, appAddress)) {
                log.error("Unauthorized to addRuntimeSecret [expectedChallenge:{}]", challenge);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        if (runtimeSecretService.getSecret(appAddress, secretIndex).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        runtimeSecretService.addSecret(appAddress, secretIndex, secretValue);
        return ResponseEntity.noContent().build();
    }

}
