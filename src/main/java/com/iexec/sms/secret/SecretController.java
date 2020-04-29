package com.iexec.sms.secret;


import com.iexec.common.security.Signature;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.web2.Web2SecretsService;
import com.iexec.sms.secret.web3.Web3Secret;
import com.iexec.sms.secret.web3.Web3SecretService;
import com.iexec.sms.utils.version.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.iexec.common.utils.SignatureUtils.signMessageHashAndGetSignature;

@Slf4j
@RestController
@RequestMapping("/secrets")
public class SecretController {

    private AuthorizationService authorizationService;
    private Web3SecretService web3SecretService;
    private VersionService versionService;
    private Web2SecretsService web2SecretsService;

    public SecretController(VersionService versionService,
                            AuthorizationService authorizationService,
                            Web2SecretsService web2SecretsService,
                            Web3SecretService web3SecretService) {
        this.versionService = versionService;
        this.web2SecretsService = web2SecretsService;
        this.authorizationService = authorizationService;
        this.web3SecretService = web3SecretService;
    }

    // Web3

    @RequestMapping(path = "/web3", method = RequestMethod.HEAD)
    public ResponseEntity isWeb3SecretSet(@RequestParam String secretAddress) {
        Optional<Web3Secret> secret = web3SecretService.getSecret(secretAddress);
        return secret.map(body -> ResponseEntity.noContent().build()).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/web3")
    public ResponseEntity<String> addWeb3Secret(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String secretAddress,
                                        @RequestBody String secretValue) {
        if (isInProduction(authorization)) {
            String challenge = authorizationService.getChallengeForSetWeb3Secret(secretAddress, secretValue);

            if (!authorizationService.isSignedByOwner(challenge, authorization, secretAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        if (web3SecretService.getSecret(secretAddress).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // secret already exists
        }

        web3SecretService.addSecret(secretAddress, secretValue);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/web3")
    public ResponseEntity<Web3Secret> getWeb3Secret(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String secretAddress,
                                        @RequestParam(required = false, defaultValue = "false") boolean shouldDecryptSecret) {
        if (isInProduction(authorization)) {
            String challenge = authorizationService.getChallengeForGetWeb3Secret(secretAddress);

            //TODO: also isAuthorizedOnExecution(..)
            if (!authorizationService.isSignedByOwner(challenge, authorization, secretAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Web3Secret> secret = web3SecretService.getSecret(secretAddress, shouldDecryptSecret);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Web2

    @RequestMapping(path = "/web2", method = RequestMethod.HEAD)
    public ResponseEntity isWeb2SecretSet(@RequestParam String ownerAddress,
                                          @RequestParam String secretAddress) {
        Optional<Secret> secret = web2SecretsService.getSecret(ownerAddress, secretAddress, false);
        return secret.map(body -> ResponseEntity.noContent().build()).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/web2")
    public ResponseEntity<Secret> getWeb2Secret(@RequestHeader("Authorization") String authorization,
                                                @RequestParam String ownerAddress,
                                                @RequestParam String secretAddress,
                                                @RequestParam(required = false, defaultValue = "false") boolean shouldDecryptSecret) {
        if (isInProduction(authorization)) {
            String challenge = authorizationService.getChallengeForGetWeb2Secret(ownerAddress, secretAddress);

            if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Secret> secret = web2SecretsService.getSecret(ownerAddress, secretAddress, shouldDecryptSecret);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/web2")
    public ResponseEntity<String> addWeb2Secret(@RequestHeader("Authorization") String authorization,
                                                @RequestParam String ownerAddress,
                                                @RequestParam String secretKey,
                                                @RequestBody String secretValue) {
        if (isInProduction(authorization)) {
            String challenge = authorizationService.getChallengeForSetWeb2Secret(ownerAddress, secretKey, secretValue);

            if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        if (web2SecretsService.getSecret(ownerAddress, secretKey).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        web2SecretsService.addSecret(ownerAddress, secretKey, secretValue);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/web2")
    public ResponseEntity<String> updateWeb2Secret(@RequestHeader("Authorization") String authorization,
                                                   @RequestParam String ownerAddress,
                                                   @RequestParam String secretKey,
                                                   @RequestBody String newSecretValue) {
        if (isInProduction(authorization)) {
            String challenge = authorizationService.getChallengeForSetWeb2Secret(ownerAddress, secretKey, newSecretValue);

            if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        if (web2SecretsService.getSecret(ownerAddress, secretKey).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        web2SecretsService.updateSecret(ownerAddress, secretKey, newSecretValue);
        return ResponseEntity.noContent().build();
    }

    /*
     * Server-side signature of a messageHash
     * */
    @PostMapping("/delegate/signature")
    private ResponseEntity signMessageHashOnServerSide(@RequestParam String messageHash,
                                                       @RequestBody String privateKey) {
        Signature signature = signMessageHashAndGetSignature(messageHash, privateKey);

        if (signature.getValue() == null || signature.getValue().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(signature.getValue());
    }

    private boolean isInProduction(String authorization) {
        boolean canAvoidAuthorization = versionService.isSnapshot() && authorization.equals("*");
        return !canAvoidAuthorization;
    }

}

