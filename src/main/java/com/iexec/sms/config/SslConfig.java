package com.iexec.sms.config;

import java.io.File;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class SslConfig {

    private SSLContext sslContext;

    public SslConfig(
            @Value("${server.ssl.key-store}") String sslKeystore,
            @Value("${server.ssl.key-store-type}") String sslKeystoreType,
            @Value("${server.ssl.key-alias}") String sslKeyAlias,
            @Value("${server.ssl.key-store-password}") String sslKeystorePassword) throws Exception {

        char[] password = sslKeystorePassword.toCharArray();
        this.sslContext = SSLContexts.custom()
            .setKeyStoreType(sslKeystoreType)
            .loadKeyMaterial(new File(sslKeystore),
                    password,
                    password,
                    (aliases, socket) -> sslKeyAlias)
            .loadTrustMaterial(null, (chain, authType) -> true)////TODO: Add CAS certificate to truststore
            .build();
    }
}
