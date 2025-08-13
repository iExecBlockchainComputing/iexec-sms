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
import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.net.URL;
import java.util.List;

@Value
@Validated
@ConfigurationProperties(prefix = "tee.scone.attestation")
@ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
public class SconeSessionSecurityConfig {
    List<String> toleratedInsecureOptions;
    List<String> ignoredSgxAdvisories;
    @NotBlank
    String mode;
    URL url;
    List<@NotNull URL> urls;

    public SconeSessionSecurityConfig(final List<String> toleratedInsecureOptions,
                                      final List<String> ignoredSgxAdvisories,
                                      final String mode,
                                      final URL url,
                                      final List<URL> urls) {
        this.toleratedInsecureOptions = toleratedInsecureOptions;
        this.ignoredSgxAdvisories = ignoredSgxAdvisories;
        this.mode = mode;
        this.url = url;
        if (urls != null && !urls.isEmpty()) {
            this.urls = urls;
        } else if (url != null) {
            this.urls = List.of(url);
        } else {
            this.urls = List.of();
        }
        if ("maa".equals(this.mode) && this.urls.isEmpty()) {
            throw new IllegalArgumentException("Attestation URL can not be null when scone session mode is 'maa'");
        }
    }

    /**
     * @deprecated use {@code tee.scone.attestation.urls} configuration property instead
     */
    @Deprecated(forRemoval = true)
    @DeprecatedConfigurationProperty(replacement = "tee.scone.attestation.urls", reason = "replaced with a list")
    public URL getUrl() {
        return this.url;
    }
}
