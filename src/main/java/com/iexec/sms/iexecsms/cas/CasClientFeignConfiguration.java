package com.iexec.sms.iexecsms.cas;

import feign.Client;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

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
    public Client feignClient() throws Exception {
        return new Client.Default(getSSLSocketFactory(), new NoopHostnameVerifier());
    }

    private SSLSocketFactory getSSLSocketFactory() throws Exception {
        TrustStrategy acceptingTrustStrategy = (chain, authType) -> true;

        char[] password = casClientConfiguration.getSslKeystorePassword().toCharArray();

        SSLContext sslContext = SSLContexts.custom()
                .setKeyStoreType("PKCS12")
                .loadKeyMaterial(new File(casClientConfiguration.getSslKeystore()), password, password, (aliases, socket) -> "localhost")
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        return sslContext.getSocketFactory();

    }

    private KeyStore getKeyStore(String file, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        File key = ResourceUtils.getFile(file);
        try (InputStream in = new FileInputStream(key)) {
            keyStore.load(in, password);
        }
        return keyStore;
    }
}
