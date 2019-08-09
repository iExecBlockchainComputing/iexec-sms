package com.iexec.sms.iexecsms.cas;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "CasClient",
        url = "#{SconeCasConfigurationService.casURL}")
public interface CasClient {

    @PostMapping("/session")
    ResponseEntity postStuffWithPalaemon(@RequestBody String palaemonFileContent) throws FeignException;
}
