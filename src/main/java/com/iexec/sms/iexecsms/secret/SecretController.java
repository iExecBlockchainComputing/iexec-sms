package com.iexec.sms.iexecsms.secret;


import com.iexec.common.security.Signature;
import com.iexec.common.sms.SmsRequest;
import com.iexec.common.sms.SmsRequestData;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.HashUtils;
import com.iexec.common.utils.SignatureUtils;
import com.iexec.sms.iexecsms.authorization.Authorization;
import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
public class SecretController {

    private static final String DOMAIN = "IEXEC_SMS_DOMAIN";//TODO: Add session salt after domain
    private SecretFolderService secretFolderService;
    private AuthorizationService authorizationService;

    public SecretController(
            SecretFolderService secretFolderService,
            AuthorizationService authorizationService) {
        this.secretFolderService = secretFolderService;
        this.authorizationService = authorizationService;
    }

    /*
     * Dev endpoint for seeing all secrets of an address
     * */
    @GetMapping("/secrets/{address}/folder")
    public ResponseEntity getSecretFolderV2(@RequestParam String address) {
        Optional<SecretFolder> secret = secretFolderService.getSecretFolder(address);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Non-required signatures for dev
     * */
    @GetMapping("/secrets/{address}/v2")
    public ResponseEntity getSecretV2(@RequestParam String address,
                                      @RequestParam String secretAlias,
                                      @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                      @RequestParam(required = false) String signature) {
        if (checkSignature) {
            byte[] message = BytesUtils.stringToBytes(HashUtils.concatenateAndHash(
                    DOMAIN,
                    address,
                    HashUtils.sha256(secretAlias)));
            Signature signatureToCheck = new Signature(signature);

            boolean isSignatureValid = SignatureUtils.isSignatureValid(message, signatureToCheck, address);

            if (!isSignatureValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        Optional<Secret> secret = secretFolderService.getSecret(address, secretAlias);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Non-required signatures for dev
     * */
    @PostMapping("/secrets/{address}/v2")
    public ResponseEntity setSecretV2(@RequestParam String address,
                                      @RequestBody Secret secret,
                                      @RequestParam(required = false, defaultValue = "false") boolean checkSignature, //dev only
                                      @RequestParam(required = false) String signature) {
        if (checkSignature) {
            byte[] message = BytesUtils.stringToBytes(HashUtils.concatenateAndHash(
                    DOMAIN,
                    address,
                    secret.getHash()));
            Signature signatureToCheck = new Signature(signature);

            boolean isSignatureValid = SignatureUtils.isSignatureValid(message, signatureToCheck, address);

            if (!isSignatureValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        boolean isSecretSet = secretFolderService.updateSecret(address, secret);
        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

}

