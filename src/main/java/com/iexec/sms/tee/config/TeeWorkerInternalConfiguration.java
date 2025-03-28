/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.config;

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@Validated
public class TeeWorkerInternalConfiguration {
    @Bean
    @ConditionalOnTeeFramework(frameworks = TeeFramework.GRAMINE)
    public Map<String, TeeServicesProperties> gramineServicesPropertiesMap(
            final TeeWorkerPipelineConfiguration pipelineConfig) {
        return pipelineConfig.getPipelines().stream()
                .map(pipeline -> pipeline.toTeeServicesProperties(null))
                .collect(Collectors.toUnmodifiableMap(
                        TeeServicesProperties::getTeeFrameworkVersion,
                        Function.identity()
                ));
    }

    @Bean
    @ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
    public Map<String, TeeServicesProperties> sconeServicesPropertiesMap(
            final TeeWorkerPipelineConfiguration pipelineConfig,
            @Value("${tee.scone.las-image}")
            @NotBlank(message = "las image must be provided") final String lasImage) {
        return pipelineConfig.getPipelines().stream()
                .map(pipeline -> pipeline.toTeeServicesProperties(lasImage))
                .collect(Collectors.toUnmodifiableMap(
                        TeeServicesProperties::getTeeFrameworkVersion,
                        Function.identity()
                ));
    }

}
