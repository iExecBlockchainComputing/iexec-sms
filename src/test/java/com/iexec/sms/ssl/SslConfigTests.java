/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.assertj.core.api.Assertions.assertThat;

class SslConfigTests {

    private static final String PASSWORD = "password";
    private static final String ALIAS = "test-alias";
    private static final String KEYSTORE_TYPE = "PKCS12";

    @TempDir
    Path tempDir;

    private SslConfig sslConfig;
    private String keystorePath;

    @BeforeEach
    void setUp() throws Exception {
        keystorePath = createTemporaryKeystore();
        sslConfig = new SslConfig(
                keystorePath,
                KEYSTORE_TYPE,
                ALIAS,
                PASSWORD.toCharArray()
        );
    }

    private String createTemporaryKeystore() throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(null, PASSWORD.toCharArray());

        File keystoreFile = tempDir.resolve("keystore.p12").toFile();
        try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
            keyStore.store(fos, PASSWORD.toCharArray());
        }
        return keystoreFile.getAbsolutePath();
    }

    @Test
    void shouldReturnValidContext() {
        SSLContext sslContext = sslConfig.getFreshSslContext();
        assertThat(sslContext).isNotNull();
    }

    @Test
    void shouldGetCorrectAttributes() {
        assertThat(sslConfig.getKeystore()).isEqualTo(keystorePath);
        assertThat(sslConfig.getKeystoreType()).isEqualTo(KEYSTORE_TYPE);
        assertThat(sslConfig.getKeyAlias()).isEqualTo(ALIAS);
        assertThat(sslConfig.getKeystorePassword()).isEqualTo(PASSWORD.toCharArray());
    }

    @Test
    void shouldReturnNullWhenKeystoreNotFound() {
        sslConfig = new SslConfig(
                "non-existing-keystore",
                KEYSTORE_TYPE,
                ALIAS,
                PASSWORD.toCharArray()
        );
        SSLContext sslContext = sslConfig.getFreshSslContext();
        assertThat(sslContext).isNull();
    }
}
