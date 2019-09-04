package com.iexec.sms.iexecsms.ssl;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Configuration
@Slf4j
public class SslRestClient {

    private SslConfiguration sslConfiguration;

    public SslRestClient(SslConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }

    public SSLContext getSslContext() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
        char[] password = sslConfiguration.getSslKeystorePassword().toCharArray();

        return SSLContexts.custom()
                .setKeyStoreType(sslConfiguration.getSslKeystoreType())
                .loadKeyMaterial(new File(sslConfiguration.getSslKeystore()),
                        password,
                        password,
                        (aliases, socket) -> sslConfiguration.getSslKeyAlias())
                .loadTrustMaterial(null, (chain, authType) -> true)////TODO: Add CAS certificate to truststore
                .build();
    }

    public RestTemplate getRestTemplate() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, IOException {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setSSLContext(getSslContext());
        clientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(clientBuilder.build());
        return new RestTemplate(factory);
    }
}
