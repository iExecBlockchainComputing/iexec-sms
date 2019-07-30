package com.iexec.sms.iexecsms.secret;


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

    @GetMapping("/secret/{owner}")
    public ResponseEntity<Secret> getSecret(@RequestParam String owner) {
        Optional<Secret> secret = secretService.getSecret(owner);

        if (secret.isPresent()) {
            return ResponseEntity.ok(secret.get());
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping("/secret/{owner}")
    public ResponseEntity setSecret(@RequestParam() String owner, @RequestBody SecretPayload secretPayload) {
        boolean isSecretSet = secretService.setSecret(Secret.builder().owner(owner).payload(secretPayload).build());

        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }

}

