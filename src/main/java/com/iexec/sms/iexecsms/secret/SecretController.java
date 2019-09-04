package com.iexec.sms.iexecsms.secret;


import com.iexec.common.utils.HashUtils;
import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import com.iexec.sms.iexecsms.secret.offchain.OffChainSecrets;
import com.iexec.sms.iexecsms.secret.offchain.OffChainSecretsService;
import com.iexec.sms.iexecsms.secret.onchain.OnChainSecret;
import com.iexec.sms.iexecsms.secret.onchain.OnChainSecretService;
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
    private OnChainSecretService onChainSecretService;
    private OffChainSecretsService offChainSecretsService;

    public SecretController(AuthorizationService authorizationService,
                            OffChainSecretsService offChainSecretsService,
                            OnChainSecretService onChainSecretService) {
        this.offChainSecretsService = offChainSecretsService;
        this.authorizationService = authorizationService;
        this.onChainSecretService = onChainSecretService;
    }


    /*
     * Non-required signatures for dev
     * */
    @GetMapping("/secrets/onchain")
    public ResponseEntity getOnChainSecret(@RequestParam String secretAddress,
                                           @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                           @RequestParam(required = false) String signature) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    secretAddress);

            //TODO: also isAuthorizedOnExecution(..)
            if (!authorizationService.isSignedByOwner(message, signature, secretAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<OnChainSecret> secret = onChainSecretService.getSecret(secretAddress);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    /*
     * Non-required signatures for dev
     * */
    @GetMapping("/secrets/offchain")
    public ResponseEntity getOffChainSecret(@RequestParam String ownerAddress,
                                            @RequestParam String secretAddress,
                                            @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                            @RequestParam(required = false) String signature) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    ownerAddress,
                    HashUtils.sha256(secretAddress));

            if (!authorizationService.isSignedByHimself(message, signature, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Secret> secret = offChainSecretsService.getSecret(ownerAddress, secretAddress);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    /*
     * Dev endpoint for seeing all secrets of an ownerAddress
     * */
    @GetMapping("/secrets/offchain/all")
    public ResponseEntity getOffChainSecrets(@RequestParam String address) {
        Optional<OffChainSecrets> secret = offChainSecretsService.getOffChainSecrets(address);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    /*
     * Non-required signatures for dev
     * */
    @PostMapping("/secrets/onchain")
    public ResponseEntity setOnChainSecret(@RequestParam String secretAddress,
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

        onChainSecretService.updateSecret(secretAddress, secretValue);
        return ResponseEntity.ok().build();
    }


    /*
     * Non-required signatures for dev
     * */
    @PostMapping("/secrets/offchain")
    public ResponseEntity setOffChainSecret(@RequestParam String ownerAddress,
                                            @RequestBody Secret secret,
                                            @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                            @RequestParam(required = false) String signature) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    ownerAddress,
                    secret.getHash());

            if (!authorizationService.isSignedByHimself(message, signature, ownerAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        boolean isSecretSet = offChainSecretsService.updateSecret(ownerAddress, secret);
        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

}

