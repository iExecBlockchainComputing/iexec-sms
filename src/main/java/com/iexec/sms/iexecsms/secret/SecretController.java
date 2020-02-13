package com.iexec.sms.iexecsms.secret;


import com.iexec.common.utils.HashUtils;
import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import com.iexec.sms.iexecsms.secret.web2.Web2Secrets;
import com.iexec.sms.iexecsms.secret.web2.Web2SecretsService;
import com.iexec.sms.iexecsms.secret.web3.Web3Secret;
import com.iexec.sms.iexecsms.secret.web3.Web3SecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
public class SecretController {

    private static final String DOMAIN = "IEXEC_SMS_DOMAIN";//TODO: Add session salt after domain
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

    @GetMapping("/secrets/web3")
    public ResponseEntity getWeb3Secret(@RequestParam String secretAddress,
                                        @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                        @RequestParam(required = false) String signature,
                                        @RequestParam(required = false, defaultValue = "false") boolean shouldDecryptSecretValue) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    secretAddress);

            //TODO: also isAuthorizedOnExecution(..)
            if (!authorizationService.isSignedByOwner(message, signature, secretAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Web3Secret> secret = web3SecretService.getSecret(secretAddress, shouldDecryptSecretValue);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/secrets/web2")
    public ResponseEntity getWeb2Secret(@RequestParam String ownerAddress,
                                        @RequestParam String secretAddress,
                                        @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                        @RequestParam(required = false) String signature,
                                        @RequestParam(required = false, defaultValue = "false") boolean shouldDecryptSecretValue) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    ownerAddress,
                    HashUtils.sha256(secretAddress));

            if (!authorizationService.isSignedByHimself(message, signature, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Secret> secret = web2SecretsService.getSecret(ownerAddress, secretAddress, shouldDecryptSecretValue);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Dev endpoint for seeing all secrets of an ownerAddress
     * */
    @GetMapping("/secrets/web2/all")
    public ResponseEntity getWeb2Secrets(@RequestParam String address,
                                         @RequestParam(required = false, defaultValue = "false") boolean shouldDecryptSecretValue) {
        Optional<Web2Secrets> secret = web2SecretsService.getWeb2Secrets(address, shouldDecryptSecretValue);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    //TODO: Refuse web3 secret updates
    @PostMapping("/secrets/web3")
    public ResponseEntity setWeb3Secret(@RequestParam String secretAddress,
                                        @RequestBody String secretValue,
                                        @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                        @RequestParam(required = false) String signature) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    secretAddress,
                    secretValue);

            if (!authorizationService.isSignedByOwner(message, signature, secretAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        web3SecretService.updateSecret(secretAddress, secretValue);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/secrets/web2")
    public ResponseEntity setWeb2Secret(@RequestParam String ownerAddress,
                                        @RequestParam String secretKey,
                                        @RequestBody String secretValue,
                                        @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                        @RequestParam(required = false) String signature) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    ownerAddress,
                    secretKey,
                    secretValue);

            if (!authorizationService.isSignedByHimself(message, signature, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        boolean isSecretSet = web2SecretsService.updateSecret(ownerAddress, new Secret(secretKey, secretValue));
        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

}

