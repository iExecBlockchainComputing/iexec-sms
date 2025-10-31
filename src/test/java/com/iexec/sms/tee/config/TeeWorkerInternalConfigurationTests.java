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

import com.iexec.sms.api.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TeeWorkerInternalConfigurationTests {
    private static final String PRE_IMAGE = "preComputeImage";
    private static final String PRE_FINGERPRINT = "preComputeFingerprint";
    private static final String PRE_ENTRYPOINT = "preComputeEntrypoint";
    private static final String POST_IMAGE = "postComputeImage";
    private static final String POST_FINGERPRINT = "postComputeFingerprint";
    private static final String POST_ENTRYPOINT = "postComputeEntrypoint";
    private static final String LAS_IMAGE = "lasImage";
    private static final String VERSION = "v5";
    private static final long HEAP_SIZE_B = 3221225472L;

    private final TeeAppProperties preComputeProperties = TeeAppProperties.builder()
            .image(PRE_IMAGE)
            .fingerprint(PRE_FINGERPRINT)
            .entrypoint(PRE_ENTRYPOINT)
            .heapSizeInBytes(HEAP_SIZE_B)
            .build();
    private final TeeAppProperties postComputeProperties = TeeAppProperties.builder()
            .image(POST_IMAGE)
            .fingerprint(POST_FINGERPRINT)
            .entrypoint(POST_ENTRYPOINT)
            .heapSizeInBytes(HEAP_SIZE_B)
            .build();

    private TeeWorkerInternalConfiguration teeWorkerInternalConfiguration;
    private TeeWorkerPipelineConfiguration pipelineConfig;

    @BeforeEach
    void setUp() {
        teeWorkerInternalConfiguration = new TeeWorkerInternalConfiguration();

        final TeeWorkerPipelineConfiguration.Pipeline pipeline = getPipeline(VERSION);
        final TeeWorkerPipelineConfiguration.Pipeline additionalPipeline = getPipeline("v6");

        pipelineConfig = new TeeWorkerPipelineConfiguration(
                List.of(pipeline, additionalPipeline)
        );
    }

    private static TeeWorkerPipelineConfiguration.Pipeline getPipeline(final String version) {
        final TeeWorkerPipelineConfiguration.StageConfig validPreComputeStageConfig = new TeeWorkerPipelineConfiguration.StageConfig(
                PRE_IMAGE,
                PRE_FINGERPRINT,
                DataSize.ofBytes(HEAP_SIZE_B),
                PRE_ENTRYPOINT
        );

        final TeeWorkerPipelineConfiguration.StageConfig validPostComputeStageConfig = new TeeWorkerPipelineConfiguration.StageConfig(
                POST_IMAGE,
                POST_FINGERPRINT,
                DataSize.ofBytes(HEAP_SIZE_B),
                POST_ENTRYPOINT
        );

        return new TeeWorkerPipelineConfiguration.Pipeline(
                version,
                validPreComputeStageConfig,
                validPostComputeStageConfig
        );
    }

    @Test
    void shouldBuildGramineServicesPropertiesMap() {
        final Map<String, TeeServicesProperties> multiPropertiesMap =
                teeWorkerInternalConfiguration.gramineServicesPropertiesMap(pipelineConfig);

        assertThat(multiPropertiesMap).isNotNull()
                .extracting(Map::size)
                .isEqualTo(2);

        assertThat(multiPropertiesMap.get(VERSION))
                .usingRecursiveComparison()
                .isEqualTo(new GramineServicesProperties(VERSION, preComputeProperties, postComputeProperties));
    }

    @Test
    void shouldBuildSconeServicesPropertiesMap() {
        final Map<String, TeeServicesProperties> multiPropertiesMap =
                teeWorkerInternalConfiguration.sconeServicesPropertiesMap(pipelineConfig, LAS_IMAGE);

        assertThat(multiPropertiesMap).isNotNull()
                .extracting(Map::size)
                .isEqualTo(2);

        assertThat(multiPropertiesMap.get(VERSION))
                .usingRecursiveComparison()
                .isEqualTo(new SconeServicesProperties(VERSION, preComputeProperties, postComputeProperties, LAS_IMAGE));
    }

    @Test
    void shouldBuildTdxServicesPropertiesMap() {
        final Map<String, TeeServicesProperties> multiPropertiesMap =
                teeWorkerInternalConfiguration.tdxServicesPropertiesMap(pipelineConfig);

        assertThat(multiPropertiesMap).isNotNull()
                .extracting(Map::size)
                .isEqualTo(2);

        assertThat(multiPropertiesMap.get(VERSION))
                .usingRecursiveComparison()
                .isEqualTo(new TdxServicesProperties(VERSION, preComputeProperties, postComputeProperties));
    }

}
