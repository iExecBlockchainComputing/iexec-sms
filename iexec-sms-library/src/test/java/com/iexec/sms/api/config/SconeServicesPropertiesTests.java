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

package com.iexec.sms.api.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SconeServicesPropertiesTests {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String FRAMEWORK_VERSION = "v5";
    private static final String LAS_IMAGE = "lasImage";
    private static final long HEAP_SIZE = 1L;

    private static final String SCONE_PRE_COMPUTE_NAME = "scone-pre-compute";
    private static final String SCONE_POST_COMPUTE_NAME = "scone-post-compute";

    private TeeAppProperties createTeeAppProperties(final String name) {
        return TeeAppProperties.builder()
                .image(name)
                .fingerprint(name + "-fingerprint")
                .entrypoint(name + "-entrypoint")
                .heapSizeInBytes(HEAP_SIZE)
                .build();
    }

    private TeeServicesProperties sconeVersionProperties;
    private SconeServicesProperties deprecatedSconeProperties;

    @BeforeEach
    void setUp() {
        final TeeAppProperties sconePreCompute = createTeeAppProperties(SCONE_PRE_COMPUTE_NAME);
        final TeeAppProperties sconePostCompute = createTeeAppProperties(SCONE_POST_COMPUTE_NAME);

        sconeVersionProperties = new SconeServicesProperties(
                FRAMEWORK_VERSION,
                sconePreCompute,
                sconePostCompute,
                LAS_IMAGE
        );
        deprecatedSconeProperties = new SconeServicesProperties(
                sconePreCompute,
                sconePostCompute,
                LAS_IMAGE
        );
    }

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        final String jsonString = mapper.writeValueAsString(sconeVersionProperties);
        final TeeServicesProperties deserializedProperties = mapper.readValue(jsonString, SconeServicesProperties.class);

        assertThat(deserializedProperties)
                .usingRecursiveComparison()
                .isEqualTo(sconeVersionProperties);
    }

    @Test
    void shouldPreserveFrameworkVersion() throws JsonProcessingException {
        final String jsonString = mapper.writeValueAsString(sconeVersionProperties);
        final TeeServicesProperties deserializedProperties = mapper.readValue(jsonString, SconeServicesProperties.class);

        assertThat(deserializedProperties.getTeeFrameworkVersion())
                .isEqualTo(FRAMEWORK_VERSION);
    }

    @Test
    void shouldHandleDeprecatedConstructor() throws JsonProcessingException {
        assertThat(deprecatedSconeProperties.getTeeFrameworkVersion()).isEmpty();
        final String jsonString = mapper.writeValueAsString(deprecatedSconeProperties);
        final SconeServicesProperties deserializedDeprecatedProperties = mapper.readValue(
                jsonString,
                SconeServicesProperties.class
        );

        assertThat(deserializedDeprecatedProperties)
                .usingRecursiveComparison()
                .isEqualTo(deprecatedSconeProperties);
        assertThat(deserializedDeprecatedProperties.getTeeFrameworkVersion()).isEmpty();
    }
}
