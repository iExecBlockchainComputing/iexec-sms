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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeeServicesPropertiesTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        List<TeeServicesProperties> teeProperties = new ArrayList<>();
        String version = "v5";
        TeeAppProperties sconePreCompute = new TeeAppProperties("scone-pre-compute", "scone-pre-compute-fingerprint", "scone-pre-compute-entrypoint", 1L);
        TeeAppProperties sconePostCompute = new TeeAppProperties("scone-post-compute", "scone-post-compute-fingerprint", "scone-post-compute-entrypoint", 1L);
        teeProperties.add(new SconeServicesProperties(version, sconePreCompute, sconePostCompute, "lasImage"));

        TeeAppProperties graminePreCompute = new TeeAppProperties("gramine-pre-compute", "gramine-pre-compute-fingerprint", "gramine-pre-compute-entrypoint", 1L);
        TeeAppProperties graminePostCompute = new TeeAppProperties("gramine-post-compute", "gramine-post-compute-fingerprint", "gramine-post-compute-entrypoint", 1L);
        teeProperties.add(new GramineServicesProperties(version, graminePreCompute, graminePostCompute));

        String jsonString = mapper.writeValueAsString(teeProperties);

        List<TeeServicesProperties> deserializedProperties = mapper.readValue(jsonString, new TypeReference<>() {
        });
        assertThat(deserializedProperties).usingRecursiveComparison().isEqualTo(teeProperties);
    }
}
