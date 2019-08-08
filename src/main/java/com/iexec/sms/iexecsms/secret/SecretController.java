package com.iexec.sms.iexecsms.secret;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SecretController {

    private SecretService secretService;

    public SecretController(SecretService secretService) {
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
    // TODO: not sure this is correct here

    /**
     * @GetMapping("/secret/{address}") public ResponseEntity<Secret> getSecret(@RequestParam String address) {
     * Optional<Secret> secret = secretService.getSecret(address);
     *
     * if (secret.isPresent()) {
     * return ResponseEntity.ok(secret.get());
     * }
     *
     * return ResponseEntity.notFound().build();
     * }
     */

    // TODO: there should be a signature from the sender to check that it is correct
    @PostMapping("/secret/{address}")
    public ResponseEntity setSecret(@RequestParam String address, @RequestBody SecretPayload secretPayload) {
        boolean isSecretSet = secretService.setSecret(Secret.builder().address(address).payload(secretPayload).build());

        if (isSecretSet) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }

}

