package com.iexec.sms.tee.session;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import feign.FeignException;

@FeignClient(name = "teeSessionClient", url = "#{teeCasConfiguration.getCasUrl()}")
public interface TeeSessionClient {

    @PostMapping(value = "/session", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> generateSecureSession(@RequestBody byte[] palaemonFile,
            @RequestHeader(name = "Content-Type", required = false, defaultValue = "application/x-www-form-urlencoded") String contentType,
            @RequestHeader(name = "Expect", required = false, defaultValue = "100-continue") String expect) throws FeignException;
}
