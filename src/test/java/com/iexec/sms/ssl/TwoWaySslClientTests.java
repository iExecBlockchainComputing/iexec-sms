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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoWaySslClientTests {

    @Mock
    private SslConfig sslConfig;
    @InjectMocks
    private TwoWaySslClient twoWaySslClient;

    @Test
    void shouldReturnConfiguredRestTemplate() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext mockSslContext = SSLContext.getInstance("TLS");
        mockSslContext.init(null, null, null);
        when(sslConfig.getFreshSslContext()).thenReturn(mockSslContext);

        twoWaySslClient = new TwoWaySslClient(sslConfig);
        RestTemplate restTemplate = twoWaySslClient.getRestTemplate();

        assertThat(restTemplate).isNotNull();
    }

}
