package com.iexec.sms.iexecsms.untee;


import com.iexec.common.security.Signature;
import com.iexec.common.sms.SmsRequest;
import com.iexec.common.sms.SmsRequestData;
import com.iexec.common.sms.secrets.SmsSecretResponse;
import com.iexec.common.sms.secrets.SmsSecretResponseData;
import com.iexec.common.sms.secrets.TaskSecrets;
import com.iexec.sms.iexecsms.authorization.Authorization;
import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import com.iexec.sms.iexecsms.untee.secret.UnTeeSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
public class UnTeeController {

    private static final String DOMAIN = "IEXEC_SMS_DOMAIN";//TODO: Add session salt after domain
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
    public ResponseEntity getUnTeeSecrets(@RequestBody SmsRequest smsRequest) {
        // Check that the demand is legitimate -> move workerSignature outside of authorization
        // see secret controller for auth
        SmsRequestData data = smsRequest.getSmsSecretRequestData();
        Authorization authorization = Authorization.builder()
                .chainTaskId(data.getChainTaskId())
                .enclaveAddress(data.getEnclaveChallenge())
                .workerAddress(data.getWorkerAddress())
                .workerSignature(new Signature(data.getWorkerSignature()))//move this
                .workerpoolSignature(new Signature(data.getCoreSignature())).build();

        if (!authorizationService.isAuthorizedOnExecution(authorization, false)) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        Optional<TaskSecrets> unTeeTaskSecrets = unTeeSecretService.getUnTeeTaskSecrets(data.getChainTaskId());
        if (unTeeTaskSecrets.isEmpty()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
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

