package com.iexec.sms.tee.session;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import feign.FeignException;

@FeignClient(name = "teeSessionClient", url = "#{teeCasConfiguration.getCasUrl()}")
public interface TeeSessionClient {

    @PostMapping(
        value = "/session",
        headers = {
            "Expect=100-continue",
            "Content-Type=application/x-www-form-urlencoded"
        })
    public ResponseEntity<String> generateSecureSession(@RequestBody byte[] palaemonFile) throws FeignException;
}
// consumes = "application/x-www-form-urlencoded",