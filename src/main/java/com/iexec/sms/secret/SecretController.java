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


import com.iexec.common.security.Signature;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.iexec.common.utils.SignatureUtils.signMessageHashAndGetSignature;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/secrets")
public class SecretController {

    private AuthorizationService authorizationService;
    private Web3SecretService web3SecretService;
    private Web2SecretsService web2SecretsService;

    public SecretController(AuthorizationService authorizationService,
                            Web2SecretsService web2SecretsService,
                            Web3SecretService web3SecretService) {
        this.web2SecretsService = web2SecretsService;
        this.authorizationService = authorizationService;
        this.web3SecretService = web3SecretService;
    }

    // Web3

    @RequestMapping(path = "/web3", method = RequestMethod.HEAD)
    public ResponseEntity<?> isWeb3SecretSet(@RequestParam String secretAddress) {
        Optional<Web3Secret> secret = web3SecretService.getSecret(secretAddress);
        return secret.map(body -> ResponseEntity.noContent().build()).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/web3")
    public ResponseEntity<Web3Secret> getWeb3Secret(@RequestHeader("Authorization") String authorization,
                                                    @RequestParam String secretAddress,
                                                    @RequestParam(required = false, defaultValue = "false") boolean shouldDecryptSecret) {
        String challenge = authorizationService.getChallengeForGetWeb3Secret(secretAddress);

        //TODO: also isAuthorizedOnExecution(..)
        if (!authorizationService.isSignedByOwner(challenge, authorization, secretAddress)) {
            log.error("Unauthorized to getWeb3Secret [expectedChallenge:{}]", challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Web3Secret> secret = web3SecretService.getSecret(secretAddress, shouldDecryptSecret);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/web3")
    public ResponseEntity<String> addWeb3Secret(@RequestHeader("Authorization") String authorization,
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

        if (web3SecretService.getSecret(secretAddress).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        web3SecretService.addSecret(secretAddress, secretValue);
        return ResponseEntity.noContent().build();
    }

    // Web2

    @RequestMapping(path = "/web2", method = RequestMethod.HEAD)
    public ResponseEntity<?> isWeb2SecretSet(@RequestParam String ownerAddress,
                                          @RequestParam String secretName) {
        Optional<Secret> secret = web2SecretsService.getSecret(ownerAddress, secretName, false);
        return secret.map(body -> ResponseEntity.noContent().build()).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/web2")
    public ResponseEntity<Secret> getWeb2Secret(@RequestHeader("Authorization") String authorization,
                                                @RequestParam String ownerAddress,
                                                @RequestParam String secretName,
                                                @RequestParam(required = false, defaultValue = "false") boolean shouldDecryptSecret) {
        String challenge = authorizationService.getChallengeForGetWeb2Secret(ownerAddress, secretName);

        if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
            log.error("Unauthorized to getWeb2Secret [expectedChallenge:{}]", challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Secret> secret = web2SecretsService.getSecret(ownerAddress, secretName, shouldDecryptSecret);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/web2")
    public ResponseEntity<String> addWeb2Secret(@RequestHeader("Authorization") String authorization,
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

        if (web2SecretsService.getSecret(ownerAddress, secretName).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        web2SecretsService.addSecret(ownerAddress, secretName, secretValue);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/web2")
    public ResponseEntity<String> updateWeb2Secret(@RequestHeader("Authorization") String authorization,
                                                   @RequestParam String ownerAddress,
                                                   @RequestParam String secretName,
                                                   @RequestBody String newSecretValue) {
        String challenge = authorizationService.getChallengeForSetWeb2Secret(ownerAddress, secretName, newSecretValue);

        if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
            log.error("Unauthorized to updateWeb2Secret [expectedChallenge:{}]", challenge);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (web2SecretsService.getSecret(ownerAddress, secretName).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        web2SecretsService.updateSecret(ownerAddress, secretName, newSecretValue);
        return ResponseEntity.noContent().build();
    }

    /*
     * Server-side signature of a messageHash
     * */
    @PostMapping("/delegate/signature")
    private ResponseEntity<String> signMessageHashOnServerSide(@RequestParam String messageHash,
                                                       @RequestBody String privateKey) {
        Signature signature = signMessageHashAndGetSignature(messageHash, privateKey);

        if (signature.getValue() == null || signature.getValue().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(signature.getValue());
    }
}

