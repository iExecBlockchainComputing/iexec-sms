package com.iexec.sms.iexecsms.cas;

import com.iexec.sms.iexecsms.ssl.SslService;
import feign.Client;
import feign.Logger;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Service
/*
 *
 * Client side authentication
 * The CAS will remember the SMS with the sms certificate (located inside the sms keystore)
 *
 * */
public class CasConfigurationService {

    private CasYmlConfiguration casYmlConfiguration;
    private SslService sslService;

    public CasConfigurationService(CasYmlConfiguration casYmlConfiguration,
                                   SslService sslService) {
        this.casYmlConfiguration = casYmlConfiguration;
        this.sslService = sslService;
    }

    public String getCasUrl() {
        return casYmlConfiguration.getUrl();
    }

    public RestTemplate getRestTemplate() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, IOException {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setSSLContext(sslService.getSslContext());
        clientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(clientBuilder.build());
        return new RestTemplate(factory);
    }

}
