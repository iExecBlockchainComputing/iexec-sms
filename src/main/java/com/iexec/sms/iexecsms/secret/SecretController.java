package com.iexec.sms.iexecsms.secret;


import com.iexec.common.utils.HashUtils;
import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import com.iexec.sms.iexecsms.secret.iexec.IexecSecret;
import com.iexec.sms.iexecsms.secret.iexec.IexecSecretService;
import com.iexec.sms.iexecsms.secret.user.UserSecrets;
import com.iexec.sms.iexecsms.secret.user.UserSecretsService;
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
    private IexecSecretService iexecSecretService;
    private UserSecretsService userSecretsService;

    public SecretController(AuthorizationService authorizationService,
                            UserSecretsService userSecretsService,
                            IexecSecretService iexecSecretService) {
        this.userSecretsService = userSecretsService;
        this.authorizationService = authorizationService;
        this.iexecSecretService = iexecSecretService;
    }

    /*
     * Dev endpoint for seeing all secrets of an address
     * */
    @GetMapping("/secrets/users/all")
    public ResponseEntity getUserSecretsV2(@RequestParam String address) {
        Optional<UserSecrets> secret = userSecretsService.getUserSecrets(address);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Non-required signatures for dev
     * */
    @GetMapping("/secrets/iexec")
    public ResponseEntity getIexecSecretV2(@RequestParam String secretAddress,
                                           @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                           @RequestParam(required = false) String signature) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    secretAddress);

            //TODO: use isAuthorized(..) and isAuthorizedOnExecution(..)
            if (!authorizationService.isSignedByOwner(message, signature, secretAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<IexecSecret> secret = iexecSecretService.getSecret(secretAddress);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Non-required signatures for dev
     * */
    @GetMapping("/secrets/user/")
    public ResponseEntity getSecretV2(@RequestParam String userAddress,
                                      @RequestParam String secretId,
                                      @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                      @RequestParam(required = false) String signature) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    userAddress,
                    HashUtils.sha256(secretId));

            if (!authorizationService.isSignedByHimself(message, signature, userAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Secret> secret = userSecretsService.getSecret(userAddress, secretId);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Non-required signatures for dev
     * */
    @PostMapping("/secrets/user")
    public ResponseEntity setSecretV2(@RequestParam String userAddress,
                                      @RequestBody Secret secret,
                                      @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                      @RequestParam(required = false) String signature) {
        if (checkSignature) {
            String message = HashUtils.concatenateAndHash(
                    DOMAIN,
                    userAddress,
                    secret.getHash());

            if (!authorizationService.isSignedByHimself(message, signature, userAddress)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        boolean isSecretSet = userSecretsService.updateSecret(userAddress, secret);
        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


    /*
     * Non-required signatures for dev
     * */
    @PostMapping("/secrets/iexec")
    public ResponseEntity setIexecSecretV2(@RequestParam String secretAddress,
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

        iexecSecretService.updateSecret(secretAddress, secretValue);
        return ResponseEntity.ok().build();
    }

}

