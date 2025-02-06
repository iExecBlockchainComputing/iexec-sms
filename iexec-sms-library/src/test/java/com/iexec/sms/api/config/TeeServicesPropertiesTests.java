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

package com.iexec.sms.api.config;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexec.commons.poco.tee.TeeFramework;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeeServicesPropertiesTests {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String FRAMEWORK_VERSION = "v5";
    private static final long HEAP_SIZE = 1L;

    private static TeeAppProperties createTeeAppProperties(String name) {
        return TeeAppProperties.builder()
                .image(name)
                .fingerprint(name + "-fingerprint")
                .entrypoint(name + "-entrypoint")
                .heapSizeInBytes(HEAP_SIZE)
                .build();
    }

    @Test
    void shouldCreateWithDeprecatedConstructor() {
        final TeeAppProperties preCompute = createTeeAppProperties("pre-compute");
        final TeeAppProperties postCompute = createTeeAppProperties("post-compute");

        TeeServicesProperties properties = new TestTeeServicesProperties(
                TeeFramework.SCONE,
                preCompute,
                postCompute
        );

        assertThat(properties.getTeeFramework()).isEqualTo(TeeFramework.SCONE);
        assertThat(properties.getTeeFrameworkVersion()).isEmpty();
        assertThat(properties.getPreComputeProperties()).isEqualTo(preCompute);
        assertThat(properties.getPostComputeProperties()).isEqualTo(postCompute);
    }

    @Test
    void shouldCreateWithNewConstructor() {
        final TeeAppProperties preCompute = createTeeAppProperties("pre-compute");
        final TeeAppProperties postCompute = createTeeAppProperties("post-compute");

        final TeeServicesProperties properties = new TestTeeServicesProperties(
                TeeFramework.SCONE,
                FRAMEWORK_VERSION,
                preCompute,
                postCompute
        );

        assertThat(properties.getTeeFramework()).isEqualTo(TeeFramework.SCONE);
        assertThat(properties.getTeeFrameworkVersion()).isEqualTo(FRAMEWORK_VERSION);
        assertThat(properties.getPreComputeProperties()).isEqualTo(preCompute);
        assertThat(properties.getPostComputeProperties()).isEqualTo(postCompute);
    }

    @Test
    void shouldSerializeWithTypeInfo() throws JsonProcessingException {
        final TeeServicesProperties properties = new TestTeeServicesProperties(
                TeeFramework.SCONE,
                FRAMEWORK_VERSION,
                createTeeAppProperties("pre-compute"),
                createTeeAppProperties("post-compute")
        );
        final String json = mapper.writeValueAsString(properties);

        assertThat(json)
                .contains("\"teeFramework\":\"SCONE\"")
                .contains("\"teeFrameworkVersion\":\"" + FRAMEWORK_VERSION + "\"")
                .contains("\"preComputeProperties\"")
                .contains("\"postComputeProperties\"");
    }

    // Test implementation of TeeServicesProperties for testing purposes
    private static class TestTeeServicesProperties extends TeeServicesProperties {
        @Deprecated(forRemoval = true)
        TestTeeServicesProperties(TeeFramework teeFramework,
                                  TeeAppProperties preComputeProperties,
                                  TeeAppProperties postComputeProperties) {
            super(teeFramework, preComputeProperties, postComputeProperties);
        }

        TestTeeServicesProperties(TeeFramework teeFramework,
                                  String teeFrameworkVersion,
                                  TeeAppProperties preComputeProperties,
                                  TeeAppProperties postComputeProperties) {
            super(teeFramework, teeFrameworkVersion, preComputeProperties, postComputeProperties);
        }
    }
}
