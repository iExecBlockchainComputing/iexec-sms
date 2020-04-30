package com.iexec.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@Slf4j
public class App {

    public static final String DOMAIN = "IEXEC_SMS_DOMAIN"; // TODO: Add session salt after domain

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        log.info("DEBUG - env: " + System.getenv().toString());//TODO: remove this later
    }

}
