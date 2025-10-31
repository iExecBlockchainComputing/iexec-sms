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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TdxServicesPropertiesTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String frameworkVersion = "v0.1";

    private final TeeServicesProperties tdxVersionProperties = new TdxServicesProperties(
            frameworkVersion, createTeeAppProperties("tdx-pre-compute"), createTeeAppProperties("tdx-post-compute"));

    private TeeAppProperties createTeeAppProperties(final String name) {
        return TeeAppProperties.builder()
                .image(name)
                .fingerprint(name + "-fingerprint")
                .entrypoint(name + "-entrypoint")
                .heapSizeInBytes(1024 * 1024 * 1024L)
                .build();
    }

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        final String jsonString = mapper.writeValueAsString(tdxVersionProperties);
        final TeeServicesProperties deserializedProperties = mapper.readValue(jsonString, TdxServicesProperties.class);

        assertThat(deserializedProperties)
                .usingRecursiveComparison()
                .isEqualTo(tdxVersionProperties);
    }

    @Test
    void shouldPreserveFrameworkVersion() throws JsonProcessingException {
        final String jsonString = mapper.writeValueAsString(tdxVersionProperties);
        final TeeServicesProperties deserializedProperties = mapper.readValue(jsonString, TdxServicesProperties.class);

        assertThat(deserializedProperties.getTeeFrameworkVersion())
                .isEqualTo(frameworkVersion);
    }

}
