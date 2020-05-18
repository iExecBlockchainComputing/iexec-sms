package com.iexec.sms.untee;


import java.util.Optional;

import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.common.sms.secret.SmsSecretResponse;
import com.iexec.common.sms.secret.SmsSecretResponseData;
import com.iexec.common.sms.secret.TaskSecrets;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.untee.secret.UnTeeSecretService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnTeeController {

    private UnTeeSecretService unTeeSecretService;
    private AuthorizationService authorizationService;

    public UnTeeController(
            AuthorizationService authorizationService,
            UnTeeSecretService unTeeSecretService) {
        this.authorizationService = authorizationService;
        this.unTeeSecretService = unTeeSecretService;
    }

    /*
     * Retrieve secrets when non-tee execution : We shouldn't do this..
     * */
    @PostMapping("/untee/secrets")
    public ResponseEntity<?> getUnTeeSecrets(@RequestHeader("Authorization") String authorization,
                                                  @RequestBody WorkerpoolAuthorization workerpoolAuthorization) {
        String workerAddress = workerpoolAuthorization.getWorkerWallet();
        String challenge = authorizationService.getChallengeForWorker(workerpoolAuthorization);
        if (!authorizationService.isSignedByHimself(challenge, authorization, workerAddress)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAuthorizedOnExecution(workerpoolAuthorization, false)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<TaskSecrets> unTeeTaskSecrets = unTeeSecretService.getUnTeeTaskSecrets(workerpoolAuthorization.getChainTaskId());
        if (unTeeTaskSecrets.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        SmsSecretResponseData smsSecretResponseData = SmsSecretResponseData.builder()
                .secrets(unTeeTaskSecrets.get())
                .build();

        SmsSecretResponse smsSecretResponse = SmsSecretResponse.builder()
                .data(smsSecretResponseData)
                .build();

        return Optional.of(smsSecretResponse).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


}

