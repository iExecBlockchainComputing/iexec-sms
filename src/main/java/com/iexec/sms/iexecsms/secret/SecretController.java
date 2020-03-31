package com.iexec.sms.iexecsms.secret;


import com.iexec.common.security.Signature;
import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import com.iexec.sms.iexecsms.secret.web2.Web2SecretsService;
import com.iexec.sms.iexecsms.secret.web3.Web3Secret;
import com.iexec.sms.iexecsms.secret.web3.Web3SecretService;
import com.iexec.sms.iexecsms.utils.version.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.iexec.common.utils.SignatureUtils.signMessageHashAndGetSignature;

@Slf4j
@RestController
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

    /*
     * Get challenges for accessing GET and POST on /secrets
     * */
    @GetMapping("/secrets/web3/challenge")
    public ResponseEntity getChallengeForGetWeb3Secret(@RequestParam String secretAddress) {
        return ResponseEntity.ok(authorizationService.getChallengeForGetWeb3Secret(secretAddress));
    }

    @GetMapping("/secrets/web2/challenge")
    public ResponseEntity getChallengeForGetWeb2Secret(@RequestParam String ownerAddress,
                                                       @RequestParam String secretAddress) {
        return ResponseEntity.ok(authorizationService.getChallengeForGetWeb2Secret(ownerAddress, secretAddress));
    }

    @PostMapping("/secrets/web3/challenge")
    public ResponseEntity getChallengeForSetWeb3Secret(@RequestParam String secretAddress,
                                                       @RequestBody String secretValue) {
        return ResponseEntity.ok(authorizationService.getChallengeForSetWeb3Secret(secretAddress, secretValue));
    }

    @PostMapping("/secrets/web2/challenge")
    public ResponseEntity getChallengeForSetWeb2Secret(@RequestParam String ownerAddress,
                                                       @RequestParam String secretKey,
                                                       @RequestBody String secretValue) {
        return ResponseEntity.ok(authorizationService.getChallengeForSetWeb2Secret(ownerAddress, secretKey, secretValue));
    }

    @GetMapping("/secrets/web3")
    public ResponseEntity getWeb3Secret(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String secretAddress,
                                        @RequestParam(required = false, defaultValue = "false") boolean shouldDisplaySecret) {
        if (isInProduction(authorization)){
            String challenge = authorizationService.getChallengeForGetWeb3Secret(secretAddress);

            //TODO: also isAuthorizedOnExecution(..)
            if (!authorizationService.isSignedByOwner(challenge, authorization, secretAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Web3Secret> secret = web3SecretService.getSecret(secretAddress, shouldDisplaySecret);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/secrets/web2")
    public ResponseEntity getWeb2Secret(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String ownerAddress,
                                        @RequestParam String secretAddress,
                                        @RequestParam(required = false, defaultValue = "false") boolean shouldDisplaySecret) {
        if (isInProduction(authorization)){
            String challenge = authorizationService.getChallengeForGetWeb2Secret(ownerAddress, secretAddress);

            if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Secret> secret = web2SecretsService.getSecret(ownerAddress, secretAddress, shouldDisplaySecret);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/secrets/web3")
    public ResponseEntity setWeb3Secret(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String secretAddress,
                                        @RequestBody String secretValue) {
        if (isInProduction(authorization)){
            String challenge = authorizationService.getChallengeForSetWeb3Secret(secretAddress, secretValue);

            if (!authorizationService.isSignedByOwner(challenge, authorization, secretAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        if (web3SecretService.getSecret(secretAddress, false).isPresent()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();//refuse web3 secret updates
        }

        web3SecretService.updateSecret(secretAddress, secretValue);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/secrets/web2")
    public ResponseEntity setWeb2Secret(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String ownerAddress,
                                        @RequestParam String secretKey,
                                        @RequestBody String secretValue) {
        if (isInProduction(authorization)){
            String challenge = authorizationService.getChallengeForSetWeb2Secret(ownerAddress, secretKey, secretValue);

            if (!authorizationService.isSignedByHimself(challenge, authorization, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        boolean isSecretSet = web2SecretsService.updateSecret(ownerAddress, new Secret(secretKey, secretValue));
        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
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

