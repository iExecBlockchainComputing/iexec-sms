package com.iexec.sms.iexecsms.cas;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class CasClientConfiguration {

    @Value("${server.ssl.key-store}")
    private String sslKeystore;

    @Value("${server.ssl.key-store-password}")
    private String sslKeystorePassword;

}
