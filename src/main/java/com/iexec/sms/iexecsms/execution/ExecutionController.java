package com.iexec.sms.iexecsms.execution;


import com.iexec.common.security.Signature;
import com.iexec.common.sms.SmsRequest;
import com.iexec.common.sms.SmsRequestData;
import com.iexec.common.sms.secrets.SmsSecretResponse;
import com.iexec.common.sms.secrets.SmsSecretResponseData;
import com.iexec.sms.iexecsms.authorization.Authorization;
import com.iexec.sms.iexecsms.authorization.AuthorizationService;
import com.iexec.sms.iexecsms.cas.CasPalaemonHelperService;
import com.iexec.sms.iexecsms.cas.CasService;
import com.iexec.sms.iexecsms.secret.offchain.OffChainSecretsService;
import com.iexec.sms.iexecsms.secret.onchain.OnChainSecretService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Keys;

import java.util.Optional;

@Slf4j
@RestController
public class ExecutionController {

    private static final String DOMAIN = "IEXEC_SMS_DOMAIN";//TODO: Add session salt after domain
    private final CasPalaemonHelperService casPalaemonHelperService;
    private final CasService casService;
    private OffChainSecretsService offChainSecretsService;
    private OnChainSecretService onChainSecretService;
    private AuthorizationService authorizationService;

    public ExecutionController(
            OffChainSecretsService offChainSecretsService,
            OnChainSecretService onChainSecretService,
            AuthorizationService authorizationService,
            CasPalaemonHelperService casPalaemonHelperService,
            CasService casService) {
        this.offChainSecretsService = offChainSecretsService;
        this.onChainSecretService = onChainSecretService;
        this.authorizationService = authorizationService;
        this.casPalaemonHelperService = casPalaemonHelperService;
        this.casService = casService;
    }

    /*
     * Retrieve secrets when non-tee execution : We shouldn't do this..
     * */
    @PostMapping("/executions/nontee/secrets")
    public ResponseEntity getNonTeeExecutionSecretsV2(@RequestBody SmsRequest smsRequest) {
        // Check that the demand is legitimate -> move workerSignature outside of authorization
        // see secret controller for auth
        SmsRequestData data = smsRequest.getSmsSecretRequestData();
        Authorization authorization = Authorization.builder()
                .chainTaskId(data.getChainTaskId())
                .enclaveAddress(data.getEnclaveChallenge())
                .workerAddress(data.getWorkerAddress())
                .workerSignature(new Signature(data.getWorkerSignature()))//move this
                .workerpoolSignature(new Signature(data.getCoreSignature())).build();

        if (!authorizationService.isAuthorizedOnExecution(authorization)) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        SmsSecretResponseData nonTeeSecrets = SmsSecretResponseData.builder().build();

        //TODO: Get Kd, Kb (& Ke? Do we really need it?)
        //onChainSecretService.getSecret()

        SmsSecretResponse smsSecretResponse = SmsSecretResponse.builder()
                .data(nonTeeSecrets)
                .build();

        return Optional.of(smsSecretResponse).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /*
     * Retrieve session when tee execution
     * */
    @PostMapping("/executions/tee/session/generate")
    public ResponseEntity generateTeeExecutionSession(@RequestBody SmsRequest smsRequest) throws Exception {
        //TODO move workerSignature outside of smsRequest (Use an authorization)
        SmsRequestData data = smsRequest.getSmsSecretRequestData();
        Authorization authorization = Authorization.builder()
                .chainTaskId(data.getChainTaskId())
                .enclaveAddress(data.getEnclaveChallenge())
                .workerAddress(data.getWorkerAddress())
                .workerSignature(new Signature(data.getWorkerSignature()))//move this
                .workerpoolSignature(new Signature(data.getCoreSignature())).build();

        if (!authorizationService.isAuthorizedOnExecution(authorization)) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        String taskId = smsRequest.getSmsSecretRequestData().getChainTaskId();
        String workerAddress = Keys.toChecksumAddress(smsRequest.getSmsSecretRequestData().getWorkerAddress());
        String attestingEnclave = smsRequest.getSmsSecretRequestData().getEnclaveChallenge();
        String sessionId = String.format("%s0000%s", RandomStringUtils.randomAlphanumeric(10), taskId);
        String configFile = casPalaemonHelperService.getPalaemonConfigurationFile(sessionId, taskId, workerAddress, attestingEnclave);
        System.out.println("## Palaemon config ##"); //dev logs, lets keep them for now
        System.out.println(configFile);
        System.out.println("#####################");

        boolean isSessionCreated = casService.generateSecureSession(configFile.getBytes());


        if (isSessionCreated) {
            return ResponseEntity.ok(sessionId);
        }

        return ResponseEntity.notFound().build();
    }

}

