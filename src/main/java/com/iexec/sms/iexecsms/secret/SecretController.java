package com.iexec.sms.iexecsms.secret;


import com.iexec.common.sms.SmsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
public class SecretController {

    private SecretService secretService;

    public SecretController(SecretService secretService) {
        this.secretService = secretService;
    }

    /**
     * Used by the NON Tee workflow:
     * https://github.com/iExecBlockchainComputing/SMS/blob/tee-scone-autonome/python/daemon.py#L280
     *
     * Note
     * Blockchain checks are already made here:
     * `authorizationService.isAuthorizedToGetKeys(authorization)` (@ResponseBody Authorization authorization)
     * `iexecHubService.isTeeTask(chainTaskId)`
     * */
    @GetMapping("/secrets/{address}")
    public ResponseEntity<Secret> getSecret(@RequestParam String address, @RequestBody SmsRequest smsRequest) {

        // TODO: check that the request is legitimate with all signatures and authorization on the blockchain

        Optional<Secret> secret = secretService.getSecret(address);
        if (secret.isPresent()) {
            return ResponseEntity.ok(secret.get());
        }

        return ResponseEntity.notFound().build();
    }


    @PostMapping("/secrets/{address}")
    public ResponseEntity setSecret(@RequestParam String address, @RequestBody SecretPayload secretPayload) {
        // TODO: there should be a signature from the sender to check that it is correct

        boolean isSecretSet = secretService.setSecret(Secret.builder().address(address).payload(secretPayload).build());

        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }

}

