/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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
import com.iexec.sms.secret.web2.NotAnExistingSecretException;
import com.iexec.sms.secret.web2.SameSecretException;
import com.iexec.sms.secret.web2.SecretAlreadyExistsException;
import com.iexec.sms.secret.web2.Web2SecretService;
import com.iexec.sms.secret.web3.Web3SecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/secrets")
public class SecretController {

    private final AuthorizationService authorizationService;
    private final Web3SecretService web3SecretService;
    private final Web2SecretService web2SecretService;

    public SecretController(AuthorizationService authorizationService,
                            Web2SecretService web2SecretService,
                            Web3SecretService web3SecretService) {
        this.web2SecretService = web2SecretService;
        this.authorizationService = authorizationService;
        this.web3SecretService = web3SecretService;
    }

    // Web3

    @RequestMapping(path = "/web3", method = RequestMethod.HEAD)
    public ResponseEntity<Void> isWeb3SecretSet(@RequestParam String secretAddress) {
        return web3SecretService.isSecretPresent(secretAddress)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/web3")
    public ResponseEntity<String> addWeb3Secret(@RequestHeader String authorization,
                                                @RequestParam String secretAddress,
                                                @RequestBody String secretValue) {
        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        String challenge = authorizationService.getChallengeForSetWeb3Secret(secretAddress, secretValue);

        if (!authorizationService.isSignedByOwner(challenge, authorization, secretAddress)) {
            log.error("Unauthorized to addWeb3Secret [expectedChallenge:{}]", challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!web3SecretService.addSecret(secretAddress, secretValue)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        return ResponseEntity.noContent().build();
    }

    // Web2

    @RequestMapping(path = "/web2", method = RequestMethod.HEAD)
    public ResponseEntity<Void> isWeb2SecretSet(@RequestParam String ownerAddress,
                                                @RequestParam String secretName) {
        return web2SecretService.isSecretPresent(ownerAddress, secretName)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/web2")
    public ResponseEntity<String> addWeb2Secret(@RequestHeader String authorization,
                                                @RequestParam String ownerAddress,
                                                @RequestParam String secretName,
                                                @RequestBody String secretValue) {
        if (!SecretUtils.isSecretSizeValid(secretValue)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        String challenge = authorizationService.getChallengeForSetWeb2Secret(ownerAddress, secretName, secretValue);

        if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
            log.error("Unauthorized to addWeb2Secret [expectedChallenge:{}]", challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            web2SecretService.addSecret(ownerAddress, secretName, secretValue);
            return ResponseEntity.noContent().build();
        } catch (SecretAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/web2")
    public ResponseEntity<String> updateWeb2Secret(@RequestHeader String authorization,
                                                   @RequestParam String ownerAddress,
                                                   @RequestParam String secretName,
                                                   @RequestBody String newSecretValue) {
        if (!SecretUtils.isSecretSizeValid(newSecretValue)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        String challenge = authorizationService.getChallengeForSetWeb2Secret(ownerAddress, secretName, newSecretValue);

        if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
            log.error("Unauthorized to updateWeb2Secret [expectedChallenge:{}]", challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            web2SecretService.updateSecret(ownerAddress, secretName, newSecretValue);
            return ResponseEntity.noContent().build();
        } catch (SameSecretException ignored) {
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException | NotAnExistingSecretException e) {
            return ResponseEntity.notFound().build();
        }
    }

}

