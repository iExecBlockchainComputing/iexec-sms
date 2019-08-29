package com.iexec.sms.iexecsms.ssl;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Configuration
@Slf4j
/*
 *
 * Client side authentication
 * The CAS will remember the SMS with the sms certificate (located inside the sms keystore)
 *
 * */
public class SslService {

    private SslYmlConfiguration sslYmlConfiguration;

    public SslService(SslYmlConfiguration sslYmlConfiguration) {
        this.sslYmlConfiguration = sslYmlConfiguration;
    }

    public SSLSocketFactory getSSLSocketFactory() throws Exception {
        SSLContext sslContext = getSslContext();
        return sslContext.getSocketFactory();
    }

    public SSLContext getSslContext() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
        TrustStrategy acceptingTrustStrategy = (chain, authType) -> true;

        char[] password = sslYmlConfiguration.getSslKeystorePassword().toCharArray();

        return SSLContexts.custom()
                .setKeyStoreType(sslYmlConfiguration.getSslKeystoreType())
                .loadKeyMaterial(new File(sslYmlConfiguration.getSslKeystore()),
                        password,
                        password,
                        (aliases, socket) -> sslYmlConfiguration.getSslKeyAlias())
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
    }
}
