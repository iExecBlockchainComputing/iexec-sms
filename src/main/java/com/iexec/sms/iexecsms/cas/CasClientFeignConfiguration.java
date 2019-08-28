package com.iexec.sms.iexecsms.cas;

import feign.Client;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;

@Configuration
@Slf4j
/*
 *
 * Client side authentication
 * The CAS will remember the SMS with the sms certificate (located inside the sms keystore)
 *
 * */
public class CasClientFeignConfiguration {

    private SslConfiguration sslConfiguration;

    public CasClientFeignConfiguration(SslConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Client feignClient() throws Exception {
        //We dont want to check CAS identity for now, used NoopHostnameVerifier(..)
        return new Client.Default(getSSLSocketFactory(), new NoopHostnameVerifier());
    }

    private SSLSocketFactory getSSLSocketFactory() throws Exception {
        TrustStrategy acceptingTrustStrategy = (chain, authType) -> true;

        char[] password = sslConfiguration.getSslKeystorePassword().toCharArray();

        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType(sslConfiguration.getSslKeystoreType())
                .loadKeyMaterial(new File(sslConfiguration.getSslKeystore()),
                        password,
                        password,
                        (aliases, socket) -> sslConfiguration.getSslKeyAlias())
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        return sslContext.getSocketFactory();
    }
}
