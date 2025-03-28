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

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
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
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
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
        assertEquals(VERSION, firstPipeline.version());

        final TeeWorkerPipelineConfiguration.StageConfig preComputeConfig = firstPipeline.preCompute();
        assertEquals(PRE_IMAGE, preComputeConfig.image());
        assertEquals(PRE_FINGERPRINT, preComputeConfig.fingerprint());
        assertEquals(PRE_HEAP_SIZE, preComputeConfig.heapSize());
        assertEquals(PRE_ENTRYPOINT, preComputeConfig.entrypoint());

        final TeeWorkerPipelineConfiguration.StageConfig postComputeConfig = firstPipeline.postCompute();
        assertEquals(POST_IMAGE, postComputeConfig.image());
        assertEquals(POST_FINGERPRINT, postComputeConfig.fingerprint());
        assertEquals(POST_HEAP_SIZE, postComputeConfig.heapSize());
        assertEquals(POST_ENTRYPOINT, postComputeConfig.entrypoint());

        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration>> violations = validator.validate(config);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailWhenPipelineConfigIsNull() {
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration>> violations = validator.validate(new TeeWorkerPipelineConfiguration(null));
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Pipeline list must not be empty");
    }

    @Test
    void shouldFailIfEmptyPipelineList() {
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration>> violations = validator.validate(new TeeWorkerPipelineConfiguration(Collections.emptyList()));
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Pipeline list must not be empty");
    }

    @Test
    void shouldFailWhenListElementsAreNull() {
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration>> violations = validator.validate(
                new TeeWorkerPipelineConfiguration(Collections.singletonList(null)));
        assertThat(violations).isNotEmpty();
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
        assertThat(violations).isNotEmpty();
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
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Pre-compute configuration must not be null",
                        "Post-compute configuration must not be null"
                );
    }

    @Test
    void shouldFailIfStageConfigFieldsAreInvalid() {
        final TeeWorkerPipelineConfiguration.StageConfig stageConfig = new TeeWorkerPipelineConfiguration.StageConfig(
                "",
                "",
                null,
                ""
        );

        final TeeWorkerPipelineConfiguration.Pipeline pipeline = new TeeWorkerPipelineConfiguration.Pipeline(
                VERSION,
                stageConfig,
                stageConfig
        );
        final TeeWorkerPipelineConfiguration config = new TeeWorkerPipelineConfiguration(
                Collections.singletonList(pipeline)
        );
        final Set<ConstraintViolation<TeeWorkerPipelineConfiguration>> configurationViolations = validator.validate(config);
        assertThat(configurationViolations).isNotEmpty();
        assertThat(configurationViolations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Heap size must not be null",
                        "Heap size must not be null",
                        "Entrypoint must not be blank",
                        "Entrypoint must not be blank",
                        "Image must not be blank",
                        "Image must not be blank",
                        "Fingerprint must not be blank",
                        "Fingerprint must not be blank"
                );
    }

    // region StageConfig.toTeeAppProperties
    @Test
    void shouldConvertStageConfigToTeeAppProperties() {
        TeeWorkerPipelineConfiguration.StageConfig stageConfig = new TeeWorkerPipelineConfiguration.StageConfig(
                PRE_IMAGE,
                PRE_FINGERPRINT,
                PRE_HEAP_SIZE,
                PRE_ENTRYPOINT
        );

        TeeAppProperties properties = stageConfig.toTeeAppProperties();

        assertThat(properties)
                .isNotNull()
                .satisfies(props -> {
                    assertEquals(PRE_IMAGE, props.getImage());
                    assertEquals(PRE_FINGERPRINT, props.getFingerprint());
                    assertEquals(PRE_HEAP_SIZE.toBytes(), props.getHeapSizeInBytes());
                    assertEquals(PRE_ENTRYPOINT, props.getEntrypoint());
                });
    }
    // endregion

    // region Pipeline.toTeeServicesProperties
    @Test
    void shouldConvertPipelineToGramineServicesProperties() {
        TeeWorkerPipelineConfiguration.Pipeline pipeline = new TeeWorkerPipelineConfiguration.Pipeline(
                VERSION,
                preCompute,
                postCompute
        );

        TeeServicesProperties properties = pipeline.toTeeServicesProperties(null);

        assertThat(properties)
                .isNotNull()
                .isInstanceOf(GramineServicesProperties.class)
                .satisfies(props -> {
                    assertEquals(TeeFramework.GRAMINE, props.getTeeFramework());

                    TeeAppProperties preProps = props.getPreComputeProperties();
                    assertEquals(PRE_IMAGE, preProps.getImage());
                    assertEquals(PRE_FINGERPRINT, preProps.getFingerprint());
                    assertEquals(PRE_HEAP_SIZE.toBytes(), preProps.getHeapSizeInBytes());
                    assertEquals(PRE_ENTRYPOINT, preProps.getEntrypoint());

                    TeeAppProperties postProps = props.getPostComputeProperties();
                    assertEquals(POST_IMAGE, postProps.getImage());
                    assertEquals(POST_FINGERPRINT, postProps.getFingerprint());
                    assertEquals(POST_HEAP_SIZE.toBytes(), postProps.getHeapSizeInBytes());
                    assertEquals(POST_ENTRYPOINT, postProps.getEntrypoint());
                });
    }

    @Test
    void shouldConvertPipelineToSconeServicesProperties() {
        String lasImage = "las-image";
        TeeWorkerPipelineConfiguration.Pipeline pipeline = new TeeWorkerPipelineConfiguration.Pipeline(
                VERSION,
                preCompute,
                postCompute
        );

        TeeServicesProperties properties = pipeline.toTeeServicesProperties(lasImage);

        assertThat(properties)
                .isNotNull()
                .isInstanceOf(SconeServicesProperties.class)
                .satisfies(props -> {
                    assertEquals(TeeFramework.SCONE, props.getTeeFramework());
                    assertEquals(lasImage, ((SconeServicesProperties) props).getLasImage());

                    TeeAppProperties preProps = props.getPreComputeProperties();
                    assertEquals(PRE_IMAGE, preProps.getImage());
                    assertEquals(PRE_FINGERPRINT, preProps.getFingerprint());
                    assertEquals(PRE_HEAP_SIZE.toBytes(), preProps.getHeapSizeInBytes());
                    assertEquals(PRE_ENTRYPOINT, preProps.getEntrypoint());

                    TeeAppProperties postProps = props.getPostComputeProperties();
                    assertEquals(POST_IMAGE, postProps.getImage());
                    assertEquals(POST_FINGERPRINT, postProps.getFingerprint());
                    assertEquals(POST_HEAP_SIZE.toBytes(), postProps.getHeapSizeInBytes());
                    assertEquals(POST_ENTRYPOINT, postProps.getEntrypoint());
                });
    }
    // endregion
}
