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

package com.iexec.sms.tee.session.scone;

import com.iexec.common.tee.TeeFramework;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
public class SconeSessionSecurityConfig {

    @Value("${tee.scone.attestation.tolerated-insecure-options}")
    @Getter
    private List<String> toleratedInsecureOptions;

    @Value("${tee.scone.attestation.ignored-sgx-advisories}")
    @Getter
    private List<String> ignoredSgxAdvisories;
}
