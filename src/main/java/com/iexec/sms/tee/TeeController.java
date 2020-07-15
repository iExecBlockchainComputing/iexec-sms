package com.iexec.sms.tee;


import java.util.Optional;

import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.tee.challenge.TeeChallenge;
import com.iexec.sms.tee.challenge.TeeChallengeService;
import com.iexec.sms.tee.session.TeeSessionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Keys;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/tee")
public class TeeController {

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
    @PostMapping("/challenges/{chainTaskId}")
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
    @PostMapping("/sessions")
    public ResponseEntity<String> generateTeeSession(@RequestHeader("Authorization") String authorization,
                                                     @RequestBody WorkerpoolAuthorization workerpoolAuthorization) {
        String workerAddress = workerpoolAuthorization.getWorkerWallet();
        String challenge = authorizationService.getChallengeForWorker(workerpoolAuthorization);
        if (!authorizationService.isSignedByHimself(challenge, authorization, workerAddress)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authorizationService.isAuthorizedOnExecution(workerpoolAuthorization, true)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String taskId = workerpoolAuthorization.getChainTaskId();
        workerAddress = Keys.toChecksumAddress(workerAddress);
        String attestingEnclave = workerpoolAuthorization.getEnclaveChallenge();

        try {
            String sessionId = teeSessionService.generateTeeSession(taskId, workerAddress, attestingEnclave);
            return !sessionId.isEmpty() ? ResponseEntity.ok(sessionId) : ResponseEntity.notFound().build();
        } catch (FeignException e) {
            log.error("Failed to generate secure session for worker [taskId:{}, workerAddress:{}, exception:{}]",
                    taskId, workerAddress, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
