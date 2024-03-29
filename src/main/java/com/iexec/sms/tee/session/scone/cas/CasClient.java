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

package com.iexec.sms.tee.session.scone.cas;

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.ssl.TwoWaySslClient;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
public class CasClient {

    private final CasConfiguration casConfiguration;
    private final TwoWaySslClient twoWaySslClient;

    public CasClient(CasConfiguration teeCasConfiguration,
                     TwoWaySslClient twoWaySslClient) {
        this.casConfiguration = teeCasConfiguration;
        this.twoWaySslClient = twoWaySslClient;
    }

    /*
     * POST /session of CAS requires 2-way SSL authentication
     */
    public ResponseEntity<String> postSession(String palaemonFile) {
        String url = casConfiguration.getUrl() + "/session";
        HttpEntity<byte[]> request = new HttpEntity<>(palaemonFile.getBytes(StandardCharsets.UTF_8));
        return twoWaySslClient
                .getRestTemplate()
                .postForEntity(url, request, String.class);
    }

}