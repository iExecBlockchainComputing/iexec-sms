/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.sms.ssl;

import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Configuration
public class SslConfig {

    private String sslKeystore;
    private String sslKeystoreType;
    private String sslKeyAlias;
    private char[] sslKeystorePasswordChar;

    public SslConfig(
            @Value("${server.ssl.key-store}") String sslKeystore,
            @Value("${server.ssl.key-store-type}") String sslKeystoreType,
            @Value("${server.ssl.key-alias}") String sslKeyAlias,
            @Value("${server.ssl.key-store-password}") String sslKeystorePassword) {
        this.sslKeystore = sslKeystore;
        this.sslKeystoreType = sslKeystoreType;
        this.sslKeyAlias = sslKeyAlias;
        this.sslKeystorePasswordChar = sslKeystorePassword.toCharArray();
    }

    /*
     * Generates new SSLContext on each call
     */
    public SSLContext getFreshSslContext() {
        try {
            return SSLContexts.custom()
                    .setKeyStoreType(sslKeystoreType)
                    .loadKeyMaterial(new File(sslKeystore),
                            sslKeystorePasswordChar,
                            sslKeystorePasswordChar,
                            (aliases, socket) -> sslKeyAlias)
                    .loadTrustMaterial(null, (chain, authType) -> true)////TODO: Add CAS certificate to truststore
                    .build();
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | CertificateException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }
}
