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
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Slf4j
@Configuration
@Validated
public class TeeWorkerInternalConfiguration {

    private TeeAppProperties buildProperties(TeeWorkerPipelineConfiguration.StageConfig config) {
        return TeeAppProperties.builder()
                .image(config.image())
                .fingerprint(config.fingerprint())
                .entrypoint(config.entrypoint())
                .heapSizeInBytes(config.heapSize().toBytes())
                .build();
    }

    public Optional<TeeServicesProperties> getPropertiesForVersion(
            final TeeFramework framework,
            final String version,
            final TeeWorkerPipelineConfiguration pipelineConfig,
            final String lasImage) {

        return pipelineConfig.getPipelines()
                .stream()
                .filter(pipeline -> version.equals(pipeline.version()))
                .findFirst()
                .map(versionedPipeline -> {
                    final TeeAppProperties preComputeProperties = buildProperties(versionedPipeline.preCompute());
                    final TeeAppProperties postComputeProperties = buildProperties(versionedPipeline.postCompute());

                    return switch (framework) {
                        case GRAMINE -> new GramineServicesProperties(preComputeProperties, postComputeProperties);
                        case SCONE ->
                                new SconeServicesProperties(preComputeProperties, postComputeProperties, lasImage);
                    };
                });
    }

    @Bean
    @ConditionalOnTeeFramework(frameworks = TeeFramework.GRAMINE)
    GramineServicesProperties gramineServicesProperties(final TeeWorkerPipelineConfiguration pipelineConfig) {
        TeeWorkerPipelineConfiguration.Pipeline defaultPipeline = pipelineConfig.getPipelines().get(0);

        TeeAppProperties preComputeProperties = buildProperties(defaultPipeline.preCompute());
        TeeAppProperties postComputeProperties = buildProperties(defaultPipeline.postCompute());

        return new GramineServicesProperties(preComputeProperties, postComputeProperties);
    }

    @Bean
    @ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
    SconeServicesProperties sconeServicesProperties(final TeeWorkerPipelineConfiguration pipelineConfig,
                                                    @Value("${tee.scone.las-image}")
                                                    @NotBlank(message = "las image must be provided") final String lasImage) {
        TeeWorkerPipelineConfiguration.Pipeline defaultPipeline = pipelineConfig.getPipelines().get(0);

        TeeAppProperties preComputeProperties = buildProperties(defaultPipeline.preCompute());
        TeeAppProperties postComputeProperties = buildProperties(defaultPipeline.postCompute());

        return new SconeServicesProperties(preComputeProperties, postComputeProperties, lasImage);
    }

}
