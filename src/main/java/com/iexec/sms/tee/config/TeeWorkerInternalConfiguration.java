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
import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Configuration
@Validated
public class TeeWorkerInternalConfiguration {
    @Bean
    TeeAppProperties preComputeProperties(@NotNull final TeeWorkerPipelineConfiguration pipelineConfig) {
        final TeeWorkerPipelineConfiguration.StageConfig preComputeConfig =
                pipelineConfig.getPipelines().get(0).preCompute();

        log.info("Pre-compute stage configured with [image={}, fingerprint={}, entrypoint={}, heapSize={}]",
                preComputeConfig.image(),
                preComputeConfig.fingerprint(),
                preComputeConfig.entrypoint(),
                preComputeConfig.heapSize());
        return TeeAppProperties.builder()
                .image(preComputeConfig.image())
                .fingerprint(preComputeConfig.fingerprint())
                .entrypoint(preComputeConfig.entrypoint())
                .heapSizeInBytes(preComputeConfig.heapSize().toBytes())
                .build();
    }

    @Bean
    TeeAppProperties postComputeProperties(@NotNull final TeeWorkerPipelineConfiguration pipelineConfig) {
        final TeeWorkerPipelineConfiguration.StageConfig postComputeConfig =
                pipelineConfig.getPipelines().get(0).postCompute();

        log.info("Post-compute stage configured with [image={}, fingerprint={}, entrypoint={}, heapSize={}]",
                postComputeConfig.image(),
                postComputeConfig.fingerprint(),
                postComputeConfig.entrypoint(),
                postComputeConfig.heapSize());
        return TeeAppProperties.builder()
                .image(postComputeConfig.image())
                .fingerprint(postComputeConfig.fingerprint())
                .entrypoint(postComputeConfig.entrypoint())
                .heapSizeInBytes(postComputeConfig.heapSize().toBytes())
                .build();
    }

    @Bean
    @ConditionalOnTeeFramework(frameworks = TeeFramework.GRAMINE)
    GramineServicesProperties gramineServicesProperties(
            final TeeAppProperties preComputeProperties,
            final TeeAppProperties postComputeProperties) {
        return new GramineServicesProperties(preComputeProperties, postComputeProperties);
    }

    @Bean
    @ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
    SconeServicesProperties sconeServicesProperties(
            final TeeAppProperties preComputeProperties,
            final TeeAppProperties postComputeProperties,
            @Value("${tee.scone.las-image}")
            @NotBlank(message = "las image must be provided")
            final String lasImage) {
        return new SconeServicesProperties(preComputeProperties, postComputeProperties, lasImage);
    }
}
