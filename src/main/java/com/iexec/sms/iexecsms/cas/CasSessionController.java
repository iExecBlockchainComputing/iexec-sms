package com.iexec.sms.iexecsms.cas;

import com.iexec.common.sms.SmsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class CasSessionController {

    private CasPalaemonHelperService casPalaemonHelperService;
    private CasService casService;

    public CasSessionController(CasPalaemonHelperService casPalaemonHelperService,
                                CasService casService) {
        this.casPalaemonHelperService = casPalaemonHelperService;
        this.casService = casService;
    }

    @PostMapping("/sessions/generate")
    public ResponseEntity generateSecureSession(@RequestBody SmsRequest smsRequest) throws Exception {
        String taskId = smsRequest.getSmsSecretRequestData().getChainTaskId();
        String workerAddress = smsRequest.getSmsSecretRequestData().getWorkerAddress();
        String attestingEnclave = smsRequest.getSmsSecretRequestData().getEnclaveChallenge();
        String configFile = casPalaemonHelperService.getPalaemonConfigurationFile(taskId, workerAddress, attestingEnclave);
        System.out.println(configFile);

        // TODO: check if we should just not simply return the sessionID
        return casService.generateSecureSessionWithPalaemonFile(configFile);
    }
}
