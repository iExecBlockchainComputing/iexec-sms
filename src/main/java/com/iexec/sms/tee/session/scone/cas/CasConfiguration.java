/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.tee.TeeFramework;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CAS: Configuration and Attestation Service.
 * It handles configurations and secret provisioning: a user uploads secrets
 * and configuration infos for a specific service to the CAS.
 * When a service wants to access those secrets, it sends a quote with its MREnclave.
 * The CAS attests the quote through Intel Attestation Service and sends the secrets
 * if the MREnclave is as expected.
 * <p>
 * MREnclave: an enclave identifier, created by hashing all its
 * code. It guarantees that a code behaves exactly as expected.
 */
@Component
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CasConfiguration {

    @Value("${tee.secret-provisioner.web.hostname}")
    private String host;

    @Value("${tee.secret-provisioner.web.port}")
    private String port;

    @Value("${tee.secret-provisioner.enclave.hostname}")
    private String publicHost;

    @Value("${tee.secret-provisioner.enclave.port}")
    private String enclavePort;

    public String getUrl() {
        return "https://" + host + ":" + port;
    }

    public String getEnclaveHost() {
        return publicHost + ":" + enclavePort;
    }
}
