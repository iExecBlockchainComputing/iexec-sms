package com.iexec.sms.config;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Client;
import feign.Logger;

@Configuration
public class FeignConfig {

    private SslConfig sslConfig;

    public FeignConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Client feignClient() {
        return new Client.Default(sslConfig.getSslContext().getSocketFactory(),
                NoopHostnameVerifier.INSTANCE);
    }
}
