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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeeWorkerInternalConfigurationTests {
    private static final String PRE_IMAGE = "preComputeImage";
    private static final String PRE_FINGERPRINT = "preComputeFingerprint";
    private static final String PRE_ENTRYPOINT = "preComputeEntrypoint";
    private static final String POST_IMAGE = "postComputeImage";
    private static final String POST_FINGERPRINT = "postComputeFingerprint";
    private static final String POST_ENTRYPOINT = "postComputeEntrypoint";
    private static final String LAS_IMAGE = "lasImage";
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

        final TeeWorkerPipelineConfiguration.Pipeline pipeline = new TeeWorkerPipelineConfiguration.Pipeline(
                "v5",
                validPreComputeStageConfig,
                validPostComputeStageConfig
        );

        pipelineConfig = new TeeWorkerPipelineConfiguration(
                Collections.singletonList(pipeline)
        );
    }

    // region getPropertiesForVersion
    @Test
    void shouldGetSconePropertiesFromVersion() {
        final TeeServicesProperties properties = teeWorkerInternalConfiguration.getPropertiesForVersion(
                TeeFramework.SCONE,
                "v5",
                pipelineConfig,
                LAS_IMAGE
        ).orElseThrow();
        assertEquals(PRE_IMAGE, properties.getPreComputeProperties().getImage());
        assertEquals(PRE_FINGERPRINT, properties.getPreComputeProperties().getFingerprint());
        assertEquals(PRE_ENTRYPOINT, properties.getPreComputeProperties().getEntrypoint());
        assertEquals(HEAP_SIZE_B, properties.getPreComputeProperties().getHeapSizeInBytes());
        assertEquals(POST_IMAGE, properties.getPostComputeProperties().getImage());
        assertEquals(POST_FINGERPRINT, properties.getPostComputeProperties().getFingerprint());
        assertEquals(POST_ENTRYPOINT, properties.getPostComputeProperties().getEntrypoint());
        assertEquals(HEAP_SIZE_B, properties.getPostComputeProperties().getHeapSizeInBytes());
    }

    @Test
    void shouldGetGraminePropertiesFromVersion() {
        final TeeServicesProperties properties = teeWorkerInternalConfiguration.getPropertiesForVersion(
                TeeFramework.GRAMINE,
                "v5",
                pipelineConfig,
                null
        ).orElseThrow();
        assertEquals(PRE_IMAGE, properties.getPreComputeProperties().getImage());
        assertEquals(PRE_FINGERPRINT, properties.getPreComputeProperties().getFingerprint());
        assertEquals(PRE_ENTRYPOINT, properties.getPreComputeProperties().getEntrypoint());
        assertEquals(HEAP_SIZE_B, properties.getPreComputeProperties().getHeapSizeInBytes());
        assertEquals(POST_IMAGE, properties.getPostComputeProperties().getImage());
        assertEquals(POST_FINGERPRINT, properties.getPostComputeProperties().getFingerprint());
        assertEquals(POST_ENTRYPOINT, properties.getPostComputeProperties().getEntrypoint());
        assertEquals(HEAP_SIZE_B, properties.getPostComputeProperties().getHeapSizeInBytes());
    }
    // endregion

    // region gramineServicesProperties
    @Test
    void shouldBuildGramineServicesProperties() {
        final GramineServicesProperties properties =
                teeWorkerInternalConfiguration.gramineServicesProperties(pipelineConfig);

        assertEquals(TeeFramework.GRAMINE, properties.getTeeFramework());
        assertEquals(preComputeProperties, properties.getPreComputeProperties());
        assertEquals(postComputeProperties, properties.getPostComputeProperties());
    }
    // endregion

    // region sconeServicesProperties
    @Test
    void shouldBuildSconeServicesProperties() {
        final SconeServicesProperties properties =
                teeWorkerInternalConfiguration.sconeServicesProperties(pipelineConfig, LAS_IMAGE);

        assertEquals(TeeFramework.SCONE, properties.getTeeFramework());
        assertEquals(preComputeProperties, properties.getPreComputeProperties());
        assertEquals(postComputeProperties, properties.getPostComputeProperties());
        assertEquals(LAS_IMAGE, properties.getLasImage());
    }
    // endregion
}
