package com.iexec.sms.iexecsms;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Slf4j
@RestController
public class AppController {

    public AppController() {
    }

    @GetMapping(value = "/up")
    public static ResponseEntity isUp() {
        String message = String.format("Up! (%s)", new Date());
        return ResponseEntity.ok(message);
    }

}

