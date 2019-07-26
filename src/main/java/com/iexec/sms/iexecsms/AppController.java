package com.iexec.sms.iexecsms;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AppController {

    public AppController() {
    }

    @GetMapping(value = "/up")
    public ResponseEntity isUp() {

        return ResponseEntity.ok("Up!");
    }

}

