package com.iexec.sms.iexecsms.ssl;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SslConfiguration {

    @Value("${server.ssl.key-store}")
    private String sslKeystore;

    @Value("${server.ssl.key-store-password}")
    private String sslKeystorePassword;

    @Value("${server.ssl.key-store-type}")
    private String sslKeystoreType;

    @Value("${server.ssl.key-alias}")
    private String sslKeyAlias;

}
