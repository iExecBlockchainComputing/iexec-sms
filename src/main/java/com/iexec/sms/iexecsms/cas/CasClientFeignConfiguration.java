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
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Configuration
@Slf4j
public class CasClientFeignConfiguration {

    private CasClientConfiguration casClientConfiguration;

    public CasClientFeignConfiguration(CasClientConfiguration casClientConfiguration) {
        this.casClientConfiguration = casClientConfiguration;
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Client feignClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException, IOException {
        return new Client.Default(getSSLSocketFactory(), new NoopHostnameVerifier());
    }

    private SSLSocketFactory getSSLSocketFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException, IOException {

        TrustStrategy acceptingTrustStrategy = (chain, authType) -> true;

        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(
                new File(casClientConfiguration.getSslKeystore()),
                casClientConfiguration.getSslKeystorePassword().toCharArray(), acceptingTrustStrategy).build();
        return sslContext.getSocketFactory();

    }
}
