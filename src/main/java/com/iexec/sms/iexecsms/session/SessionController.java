package com.iexec.sms.iexecsms.session;


import com.iexec.common.sms.SmsRequest;
import com.iexec.common.sms.scone.SconeSecureSessionResponse;
import com.iexec.sms.iexecsms.PalaemonHelperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SessionController {

    private PalaemonHelperService palaemonHelperService;

    public SessionController(PalaemonHelperService palaemonHelperService) {
        this.palaemonHelperService = palaemonHelperService;
    }

    @PostMapping("/sessions/generate")
    public ResponseEntity<SconeSecureSessionResponse> generateSecureSession(@RequestBody SmsRequest smsRequest) throws Exception {

        String taskId = smsRequest.getSmsSecretRequestData().getChainTaskId();
        String workerAddress = smsRequest.getSmsSecretRequestData().getWorkerAddress();
        String configFile = palaemonHelperService.getPalaemonConfigurationFile(taskId, workerAddress);
        System.out.println(configFile);

        // TODO: send all to the CAS to generate the session
        
        return ResponseEntity.notFound().build();
    }
}
