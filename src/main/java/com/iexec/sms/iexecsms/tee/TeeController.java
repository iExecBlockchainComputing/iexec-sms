package com.iexec.sms.iexecsms.tee;


import com.iexec.common.security.Signature;
import com.iexec.common.sms.SmsRequest;
import com.iexec.common.sms.SmsRequestData;
import com.iexec.sms.iexecsms.authorization.Authorization;
import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import com.iexec.sms.iexecsms.tee.challenge.TeeChallenge;
import com.iexec.sms.iexecsms.tee.challenge.TeeChallengeService;
import com.iexec.sms.iexecsms.tee.session.TeeSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Keys;

import java.util.Optional;

@Slf4j
@RestController
public class TeeController {

    private static final String DOMAIN = "IEXEC_SMS_DOMAIN";//TODO: Add session salt after domain
    private AuthorizationService authorizationService;
    private TeeChallengeService teeChallengeService;
    private TeeSessionService teeSessionService;

    public TeeController(
            AuthorizationService authorizationService,
            TeeChallengeService teeChallengeService,
            TeeSessionService teeSessionService) {
        this.authorizationService = authorizationService;
        this.teeChallengeService = teeChallengeService;
        this.teeSessionService = teeSessionService;
    }

    /**
     * Called by the core, not the worker
     */
    @PostMapping("/tee/challenges/{chainTaskId}")
    public ResponseEntity<String> generateTeeChallenge(@PathVariable String chainTaskId) {
        Optional<TeeChallenge> optionalExecutionChallenge = teeChallengeService.getOrCreate(chainTaskId, false);

        return optionalExecutionChallenge.map(teeChallenge -> ResponseEntity.ok(teeChallenge.getCredentials().getAddress()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     *
     * Get and create session on tee execution
     * The worker will connect to the CAS with the sessionId to retrieve all secrets
     *
     * */
    @PostMapping("tee/sessions")
    public ResponseEntity generateTeeSession(@RequestBody SmsRequest smsRequest) {
        //TODO move workerSignature outside of smsRequest (Use an authorization)
        SmsRequestData data = smsRequest.getSmsSecretRequestData();
        Authorization authorization = Authorization.builder()
                .chainTaskId(data.getChainTaskId())
                .enclaveAddress(data.getEnclaveChallenge())
                .workerAddress(data.getWorkerAddress())
                .workerSignature(new Signature(data.getWorkerSignature()))//move this
                .workerpoolSignature(new Signature(data.getCoreSignature())).build();

        if (!authorizationService.isAuthorizedOnExecution(authorization, true)) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        String taskId = smsRequest.getSmsSecretRequestData().getChainTaskId();
        String workerAddress = Keys.toChecksumAddress(smsRequest.getSmsSecretRequestData().getWorkerAddress());
        String attestingEnclave = smsRequest.getSmsSecretRequestData().getEnclaveChallenge();

        String sessionId = teeSessionService.generateTeeSession(taskId, workerAddress, attestingEnclave);

        if (!sessionId.isEmpty()) {
            return ResponseEntity.ok(sessionId);
        }

        return ResponseEntity.notFound().build();
    }


}

