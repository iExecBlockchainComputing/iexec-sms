/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.common.utils.FeignBuilder;
import com.iexec.sms.tee.ConditionalOnTeeProvider;
import feign.Logger.Level;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnTeeProvider(providers = TeeEnclaveProvider.GRAMINE)
@Getter
public class SpsConfiguration {
    @Value("${tee.secret-provisioner.web.hostname}")
    private String webHost;

    @Value("${tee.secret-provisioner.web.port}")
    private String webPort;

    @Value("${tee.gramine.sps.login}")
    private String webLogin;

    @Value("${tee.gramine.sps.password}")
    private String webPassword;

    @Value("${tee.secret-provisioner.enclave.hostname}")
    private String enclaveHostName;

    @Value("${tee.secret-provisioner.enclave.port}")
    private String enclavePort;

    private SpsApiClient spsApiClient;

    public String getWebUrl() {
        return "http://" + webHost + ":" + webPort;
    }

    public String getEnclaveHost() {
        return enclaveHostName + ":" + enclavePort;
    }

    public SpsApiClient getInstance() {
        if (spsApiClient == null) {
            spsApiClient = FeignBuilder.createBuilderWithBasicAuth(Level.FULL,
                    webLogin, webPassword)
                    .target(SpsApiClient.class, getWebUrl());
        }
        return spsApiClient;
    }

}
