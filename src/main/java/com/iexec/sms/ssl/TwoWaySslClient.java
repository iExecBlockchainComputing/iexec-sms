package com.iexec.sms.ssl;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TwoWaySslClient {

    private final SslConfig sslConfig;

    public TwoWaySslClient(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    /*
     * Using RestTemplate for connections requiring 2-way SSL authentication
     * It needs a fresh sslContext on each call (see getFreshSslContext()).
     * If you don't do this, you will get a 200 response on the first call followed by a 401 response on next calls
     *
     * Note: currently not able to avoid 401 responses with feignClient (see feignClient() method starter below)
     * */
    public RestTemplate getRestTemplate() {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setSSLContext(sslConfig.getFreshSslContext());
        clientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(clientBuilder.build());
        return new RestTemplate(factory);
    }

    /*
    @Bean
    public Client feignClient() {
        return new Client.Default(sslConfig.getFreshSslContext().getSocketFactory(), NoopHostnameVerifier.INSTANCE);
    }
    */

}
