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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    private final TeeWorkerPipelineConfiguration.StageConfig preCompute = new TeeWorkerPipelineConfiguration.StageConfig(
            PRE_IMAGE,
            PRE_FINGERPRINT,
            PRE_HEAP_SIZE,
            PRE_ENTRYPOINT);
    private final TeeWorkerPipelineConfiguration.StageConfig postCompute = new TeeWorkerPipelineConfiguration.StageConfig(
            POST_IMAGE,
            POST_FINGERPRINT,
            POST_HEAP_SIZE,
            POST_ENTRYPOINT);

    private Validator validator;

    @BeforeEach
    void setUp() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldGetPipelineConfiguration() {
        final TeeWorkerPipelineConfiguration.Pipeline pipeline = new TeeWorkerPipelineConfiguration.Pipeline(
                VERSION,
                preCompute,
                postCompute
        );
        final TeeWorkerPipelineConfiguration config = new TeeWorkerPipelineConfiguration(Collections.singletonList(pipeline));

        assertEquals(1, config.getPipelines().size());
        final TeeWorkerPipelineConfiguration.Pipeline firstPipeline = config.getPipelines().get(0);
        assertEquals(VERSION, firstPipeline.getVersion());

        final TeeWorkerPipelineConfiguration.StageConfig preComputeConfig = firstPipeline.getPreCompute();
        assertEquals(PRE_IMAGE, preComputeConfig.image());
        assertEquals(PRE_FINGERPRINT, preComputeConfig.fingerprint());
        assertEquals(PRE_HEAP_SIZE, preComputeConfig.heapSize());
        assertEquals(PRE_ENTRYPOINT, preComputeConfig.entrypoint());

        final TeeWorkerPipelineConfiguration.StageConfig postComputeConfig = firstPipeline.getPostCompute();
        assertEquals(POST_IMAGE, postComputeConfig.image());
        assertEquals(POST_FINGERPRINT, postComputeConfig.fingerprint());
        assertEquals(POST_HEAP_SIZE, postComputeConfig.heapSize());
        assertEquals(POST_ENTRYPOINT, postComputeConfig.entrypoint());
    }

    @Test
    void shouldFailWhenPipelineConfigIsNull() {
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration>> violations = validator.validate(new TeeWorkerPipelineConfiguration(null));
        assertFalse(violations.isEmpty());
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Pipeline list must not be empty");
    }

    @Test
    void shouldFailIfEmptyPipelineList() {
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration>> violations = validator.validate(new TeeWorkerPipelineConfiguration(Collections.emptyList()));
        assertFalse(violations.isEmpty());
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Pipeline list must not be empty");
    }

    @Test
    void shouldFailWhenListElementsAreNull() {
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration>> violations = validator.validate(
                new TeeWorkerPipelineConfiguration(Collections.singletonList(null)));
        assertFalse(violations.isEmpty());
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("List elements must not be null");
    }

    @Test
    void shouldFailIfBlankVersion() {
        final TeeWorkerPipelineConfiguration.Pipeline pipeline = new TeeWorkerPipelineConfiguration.Pipeline(
                "",
                preCompute,
                postCompute
        );
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration.Pipeline>> violations = validator.validate(pipeline);
        assertFalse(violations.isEmpty());
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Pipeline version must not be blank");
    }

    @Test
    void shouldFailIfNullStageConfig() {
        final TeeWorkerPipelineConfiguration.Pipeline pipeline = new TeeWorkerPipelineConfiguration.Pipeline(
                VERSION,
                null,
                null
        );
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration.Pipeline>> violations = validator.validate(pipeline);
        assertEquals(2, violations.size());
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Pre-compute configuration must not be null",
                        "Post-compute configuration must not be null");
    }

    @Test
    void shouldFailIfStageConfigFieldsAreInvalid() {
        final TeeWorkerPipelineConfiguration.StageConfig config = new TeeWorkerPipelineConfiguration.StageConfig(
                "",
                "",
                null,
                ""
        );
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration.StageConfig>> violations = validator.validate(config);
        assertEquals(4, violations.size());
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Fingerprint must not be blank",
                        "Image must not be blank",
                        "Heap size must not be null",
                        "Entrypoint must not be blank");
    }
}
