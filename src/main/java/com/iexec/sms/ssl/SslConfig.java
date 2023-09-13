/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContexts;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Slf4j
@Value
@ConstructorBinding
@ConfigurationProperties(prefix = "tee.ssl")
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
public class SslConfig {

    String keystore;
    String keystoreType;
    String keyAlias;
    char[] keystorePassword;

    /*
     * Generates new SSLContext on each call
     */
    public SSLContext getFreshSslContext() {
        try {
            return SSLContexts.custom()
                    .setKeyStoreType(keystoreType)
                    .loadKeyMaterial(new File(keystore),
                            keystorePassword,
                            keystorePassword,
                            (aliases, socket) -> keyAlias)
                    .loadTrustMaterial(null, (chain, authType) -> true)////TODO: Add CAS certificate to truststore
                    .build();
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | CertificateException | KeyManagementException e) {
            log.warn("Failed to create a fresh SSL context", e);
        }
        return null;
    }
}
