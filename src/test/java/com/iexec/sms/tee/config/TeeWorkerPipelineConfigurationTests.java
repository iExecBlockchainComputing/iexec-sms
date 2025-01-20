/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeeWorkerPipelineConfigurationTests {
    private static final String VERSION = "v5";
    private static final String PRE_IMAGE = "preImage";
    private static final String PRE_FINGERPRINT = "preFingerprint";
    private static final DataSize PRE_HEAP_SIZE = DataSize.parse("3GB");
    private static final String PRE_ENTRYPOINT = "preEntrypoint";
    private static final String POST_IMAGE = "postImage";
    private static final String POST_FINGERPRINT = "postFingerprint";
    private static final DataSize POST_HEAP_SIZE = DataSize.parse("3GB");
    private static final String POST_ENTRYPOINT = "postEntrypoint";

    @Mock
    private TeeWorkerPipelineConfiguration.Pipeline pipeline;

    @Mock
    private TeeWorkerPipelineConfiguration.StageConfig preCompute;

    @Mock
    private TeeWorkerPipelineConfiguration.StageConfig postCompute;

    private TeeWorkerPipelineConfiguration pipelineConfiguration;

    @BeforeEach
    void setUp() {
        pipelineConfiguration = new TeeWorkerPipelineConfiguration();
        when(pipeline.getVersion()).thenReturn(VERSION);
        when(pipeline.getPreCompute()).thenReturn(preCompute);
        when(pipeline.getPostCompute()).thenReturn(postCompute);

        // Pre-compute stage configuration
        when(preCompute.getImage()).thenReturn(PRE_IMAGE);
        when(preCompute.getFingerprint()).thenReturn(PRE_FINGERPRINT);
        when(preCompute.getHeapSize()).thenReturn(PRE_HEAP_SIZE);
        when(preCompute.getEntrypoint()).thenReturn(PRE_ENTRYPOINT);

        // Post-compute stage configuration
        when(postCompute.getImage()).thenReturn(POST_IMAGE);
        when(postCompute.getFingerprint()).thenReturn(POST_FINGERPRINT);
        when(postCompute.getHeapSize()).thenReturn(POST_HEAP_SIZE);
        when(postCompute.getEntrypoint()).thenReturn(POST_ENTRYPOINT);

        pipelineConfiguration.setPipelines(Collections.singletonList(pipeline));
    }

    @Test
    void shouldGetPipelineConfiguration() {
        assertEquals(1, pipelineConfiguration.getPipelines().size());

        TeeWorkerPipelineConfiguration.Pipeline firstPipeline = pipelineConfiguration.getPipelines().get(0);
        assertEquals(VERSION, firstPipeline.getVersion());

        TeeWorkerPipelineConfiguration.StageConfig preComputeConfig = firstPipeline.getPreCompute();
        assertEquals(PRE_IMAGE, preComputeConfig.getImage());
        assertEquals(PRE_FINGERPRINT, preComputeConfig.getFingerprint());
        assertEquals(PRE_HEAP_SIZE, preComputeConfig.getHeapSize());
        assertEquals(PRE_ENTRYPOINT, preComputeConfig.getEntrypoint());

        TeeWorkerPipelineConfiguration.StageConfig postComputeConfig = firstPipeline.getPostCompute();
        assertEquals(POST_IMAGE, postComputeConfig.getImage());
        assertEquals(POST_FINGERPRINT, postComputeConfig.getFingerprint());
        assertEquals(POST_HEAP_SIZE, postComputeConfig.getHeapSize());
        assertEquals(POST_ENTRYPOINT, postComputeConfig.getEntrypoint());
    }
}
