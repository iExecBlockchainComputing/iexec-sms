package com.iexec.sms.iexecsms.cas;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@FeignClient(name = "CasClient",
        url = "#{casConfigurationService.casUrl}", configuration = CasConfigurationService.class)
public interface CasClient {

    @PostMapping(value = "/session", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity generateSecureSessionWithPalaemonFile(@RequestBody byte[] data) throws FeignException;
}
