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

package com.iexec.sms.tee.session.gramine.sps;

import com.iexec.sms.tee.session.generic.TeeSessionStorageClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

@Service
public class SpsClient implements TeeSessionStorageClient {

    private final SpsConfiguration spsConfiguration;

    public SpsClient(SpsConfiguration spsConfiguration) {
        this.spsConfiguration = spsConfiguration;
    }

    public ResponseEntity<String> postSession(byte[] sessionFile) {
        String url = spsConfiguration.getWebUrl() + "/api/session";
        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Basic " +
                HttpHeaders.encodeBasicAuth(
                        spsConfiguration.getWebLogin(),
                        spsConfiguration.getWebPassword(),
                        Charset.defaultCharset()));

        HttpEntity<byte[]> request = new HttpEntity<>(sessionFile, httpHeaders);
        return createRestTemplate()
                .postForEntity(url, request, String.class);
    }

    RestTemplate createRestTemplate() {
        return new RestTemplate();
    }
}