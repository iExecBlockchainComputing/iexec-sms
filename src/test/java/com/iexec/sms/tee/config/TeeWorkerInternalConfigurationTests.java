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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeeWorkerInternalConfigurationTests {
    private static final String IMAGE = "image";
    private static final String FINGERPRINT = "fingerprint";
    private static final String ENTRYPOINT = "entrypoint";
    private static final String LAS_IMAGE = "lasImage";
    private static final DataSize HEAP_SIZE = DataSize.parse("3GB");
    private static final long HEAP_SIZE_B = 3221225472L;

    private TeeWorkerInternalConfiguration teeWorkerInternalConfiguration;
    private TeeWorkerPipelineConfiguration pipelineConfig;

    @BeforeEach
    void setUp() {
        teeWorkerInternalConfiguration = new TeeWorkerInternalConfiguration();

        // Setup pipeline configuration
        TeeWorkerPipelineConfiguration.StageConfig stageConfig = new TeeWorkerPipelineConfiguration.StageConfig();
        stageConfig.setImage(IMAGE);
        stageConfig.setFingerprint(FINGERPRINT);
        stageConfig.setHeapSize(HEAP_SIZE);
        stageConfig.setEntrypoint(ENTRYPOINT);

        TeeWorkerPipelineConfiguration.Pipeline pipeline = new TeeWorkerPipelineConfiguration.Pipeline();
        pipeline.setVersion("v5");
        pipeline.setPreCompute(stageConfig);
        pipeline.setPostCompute(stageConfig);

        pipelineConfig = new TeeWorkerPipelineConfiguration();
        pipelineConfig.setPipelines(Collections.singletonList(pipeline));
    }

    // region validatePipelineConfig
    @Test
    void shouldFailPreComputePropertiesWhenPipelineConfigIsNull() {
        assertThrows(IllegalStateException.class,
                () -> teeWorkerInternalConfiguration.preComputeProperties(null));
    }

    @Test
    void shouldFailPreComputePropertiesWhenPipelinesIsNull() {
        pipelineConfig.setPipelines(null);
        assertThrows(IllegalStateException.class,
                () -> teeWorkerInternalConfiguration.preComputeProperties(pipelineConfig));
    }

    @Test
    void shouldFailPreComputePropertiesWhenFirstPipelineIsNull() {
        pipelineConfig.setPipelines(Collections.singletonList(null));
        assertThrows(IllegalStateException.class,
                () -> teeWorkerInternalConfiguration.preComputeProperties(pipelineConfig));
    }
    // endregion

    // region preComputeProperties
    @Test
    void shouldGetPreComputePropertiesFromPipeline() {
        TeeAppProperties properties = teeWorkerInternalConfiguration.preComputeProperties(pipelineConfig);

        assertEquals(IMAGE, properties.getImage());
        assertEquals(FINGERPRINT, properties.getFingerprint());
        assertEquals(ENTRYPOINT, properties.getEntrypoint());
        assertEquals(HEAP_SIZE_B, properties.getHeapSizeInBytes());
    }

    @Test
    void shouldFailPreComputePropertiesWhenNoPipeline() {
        pipelineConfig.setPipelines(Collections.emptyList());
        assertThrows(IllegalStateException.class,
                () -> teeWorkerInternalConfiguration.preComputeProperties(pipelineConfig));
    }
    // endregion

    // region postComputeProperties
    @Test
    void shouldGetPostComputePropertiesFromPipeline() {
        TeeAppProperties properties = teeWorkerInternalConfiguration.postComputeProperties(pipelineConfig);

        assertEquals(IMAGE, properties.getImage());
        assertEquals(FINGERPRINT, properties.getFingerprint());
        assertEquals(ENTRYPOINT, properties.getEntrypoint());
        assertEquals(HEAP_SIZE_B, properties.getHeapSizeInBytes());
    }

    @Test
    void shouldFailPostComputePropertiesWhenNoPipeline() {
        pipelineConfig.setPipelines(Collections.emptyList());
        assertThrows(IllegalStateException.class,
                () -> teeWorkerInternalConfiguration.postComputeProperties(pipelineConfig));
    }
    // endregion

    // region gramineServicesProperties
    @Test
    void shouldBuildGramineServicesProperties() {
        TeeAppProperties preComputeProperties = TeeAppProperties.builder()
                .image("preComputeImage")
                .fingerprint("preComputeFingerprint")
                .entrypoint("preComputeEntrypoint")
                .heapSizeInBytes(1L)
                .build();
        TeeAppProperties postComputeProperties = TeeAppProperties.builder()
                .image("postComputeImage")
                .fingerprint("postComputeFingerprint")
                .entrypoint("postComputeEntrypoint")
                .heapSizeInBytes(1L)
                .build();

        GramineServicesProperties properties =
                teeWorkerInternalConfiguration.gramineServicesProperties(preComputeProperties, postComputeProperties);

        assertEquals(TeeFramework.GRAMINE, properties.getTeeFramework());
        assertEquals(preComputeProperties, properties.getPreComputeProperties());
        assertEquals(postComputeProperties, properties.getPostComputeProperties());
    }
    // endregion

    // region sconeServicesProperties
    @Test
    void shouldBuildSconeServicesProperties() {
        TeeAppProperties preComputeProperties = TeeAppProperties.builder()
                .image("preComputeImage")
                .fingerprint("preComputeFingerprint")
                .entrypoint("preComputeEntrypoint")
                .heapSizeInBytes(1L)
                .build();
        TeeAppProperties postComputeProperties = TeeAppProperties.builder()
                .image("postComputeImage")
                .fingerprint("postComputeFingerprint")
                .entrypoint("postComputeEntrypoint")
                .heapSizeInBytes(1L)
                .build();

        SconeServicesProperties properties =
                teeWorkerInternalConfiguration.sconeServicesProperties(preComputeProperties, postComputeProperties, LAS_IMAGE);

        assertEquals(TeeFramework.SCONE, properties.getTeeFramework());
        assertEquals(preComputeProperties, properties.getPreComputeProperties());
        assertEquals(postComputeProperties, properties.getPostComputeProperties());
        assertEquals(LAS_IMAGE, properties.getLasImage());
    }
    // endregion
}