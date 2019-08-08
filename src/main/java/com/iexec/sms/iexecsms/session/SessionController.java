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

        return ResponseEntity.notFound().build();
    }
}
