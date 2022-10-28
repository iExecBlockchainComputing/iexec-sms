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
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/secrets")
public class SecretController {

    private final AuthorizationService authorizationService;
    private final Web3SecretService web3SecretService;
    private final Web2SecretsService web2SecretsService;

    public SecretController(AuthorizationService authorizationService,
                            Web2SecretsService web2SecretsService,
                            Web3SecretService web3SecretService) {
        this.web2SecretsService = web2SecretsService;
        this.authorizationService = authorizationService;
        this.web3SecretService = web3SecretService;
    }

    // Web3

    @RequestMapping(path = "/web3", method = RequestMethod.HEAD)
    public ResponseEntity<Void> isWeb3SecretSet(@RequestParam String secretAddress) {
        Optional<Web3Secret> secret = web3SecretService.getSecret(secretAddress);
        return secret.isPresent() ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
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
        Optional<Secret> secret = web2SecretsService.getSecret(ownerAddress, secretName);
        return secret.isPresent() ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
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

        if (!web2SecretsService.addSecret(ownerAddress, secretName, secretValue)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.noContent().build();
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
            web2SecretsService.updateSecret(ownerAddress, secretName, newSecretValue);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

}

