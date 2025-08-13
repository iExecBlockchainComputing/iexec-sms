/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

@Value
@ConfigurationProperties(prefix = "tee.scone.attestation")
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
public class SconeSessionSecurityConfig {
    List<String> toleratedInsecureOptions;
    List<String> ignoredSgxAdvisories;
    @NotBlank
    String mode;
    @Deprecated(forRemoval = true)
    URI url;
    List<URI> urls;

    public SconeSessionSecurityConfig(final List<String> toleratedInsecureOptions,
                                      final List<String> ignoredSgxAdvisories,
                                      final String mode,
                                      final URI url,
                                      final List<URI> urls) {
        this.toleratedInsecureOptions = toleratedInsecureOptions;
        this.ignoredSgxAdvisories = ignoredSgxAdvisories;
        this.mode = mode;
        this.url = url;
        this.urls = urls;
        if ("maa".equals(this.mode) && this.url == null && this.urls.isEmpty()) {
            throw new IllegalArgumentException("Attestation URL can not be null when scone session mode is 'maa'");
        }
    }
}
