package com.iexec.sms.iexecsms.secret;


import com.iexec.common.security.Signature;
import com.iexec.common.sms.SmsRequest;
import com.iexec.common.sms.SmsRequestData;
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

    private SecretService secretService;
    private AuthorizationService authorizationService;

    public SecretController(SecretService secretService,
                            AuthorizationService authorizationService) {
        this.secretService = secretService;
        this.authorizationService = authorizationService;
    }

    /**
     * Used by the NON Tee workflow:
     * https://github.com/iExecBlockchainComputing/SMS/blob/tee-scone-autonome/python/daemon.py#L280
     * <p>
     * Note
     * Blockchain checks are already made here:
     * `authorizationService.isAuthorizedToGetKeys(authorization)` (@ResponseBody Authorization authorization)
     * `iexecHubService.isTeeTask(chainTaskId)`
     *
     * @return
     */
    @GetMapping("/secrets/{address}")
    public ResponseEntity getSecret(@RequestParam String address, @RequestBody SmsRequest smsRequest) {

        // Check that the demand is legitimate
        SmsRequestData data = smsRequest.getSmsSecretRequestData();
        Authorization authorization = Authorization.builder()
                .chainTaskId(data.getChainTaskId())
                .enclaveAddress(data.getEnclaveChallenge())
                .workerAddress(data.getWorkerAddress())
                .workerSignature(new Signature(data.getWorkerSignature()))
                .workerpoolSignature(new Signature(data.getCoreSignature())).build();

        if (!authorizationService.isAuthorizedToGetKeys(authorization)) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        Optional<Secret> secret = secretService.getSecret(address);
        return secret.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PostMapping("/secrets/{address}")
    public ResponseEntity setSecret(@RequestParam String address, @RequestBody SecretPayload secretPayload) {
        // TODO: should there be a signature from the sender to check that it is correct ?

        boolean isSecretSet = secretService.setSecret(Secret.builder().address(address).payload(secretPayload).build());
        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

}

