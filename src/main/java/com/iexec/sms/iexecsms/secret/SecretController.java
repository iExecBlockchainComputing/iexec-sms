package com.iexec.sms.iexecsms.secret;


import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
public class SecretController {

    private AuthorizationService authorizationService;
    private SecretService secretService;

    public SecretController(AuthorizationService authorizationService, SecretService secretService) {
        this.authorizationService = authorizationService;
        this.secretService = secretService;
    }

    /*
     *
     * Handle workflow when is/isnt tee task :
     * https://github.com/iExecBlockchainComputing/SMS/blob/tee-scone-autonome/python/daemon.py#L280
     *
     * Note
     * Blockchain checks are already made here:
     * `authorizationService.isAuthorizedToGetKeys(authorization)` (@ResponseBody Authorization authorization)
     * `iexecHubService.isTeeTask(chainTaskId)`
     * */
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

