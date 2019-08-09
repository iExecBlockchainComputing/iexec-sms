package com.iexec.sms.iexecsms.session;


import com.iexec.common.sms.SmsRequest;
import com.iexec.common.sms.scone.SconeSecureSessionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SessionController {

    @PostMapping("/sessions/generate")
    public ResponseEntity<SconeSecureSessionResponse> generateSecureSession(@RequestBody SmsRequest smsRequest) {

        // TODO contact the CAS and perform whatever is needed
        // This is here that the palaemon configuration file should be created and sent to the cas

        // Palaemon file should be generated and a call to the CAS with this file should happen here.



        return ResponseEntity.notFound().build();
    }
}
