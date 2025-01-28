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
        TeeAppProperties sconePreCompute = new TeeAppProperties("scone-pre-compute", "scone-pre-compute-fingerprint", "scone-pre-compute-entrypoint", 1L);
        TeeAppProperties sconePostCompute = new TeeAppProperties("scone-post-compute", "scone-post-compute-fingerprint", "scone-post-compute-entrypoint", 1L);
        teeProperties.add(new SconeServicesProperties(sconePreCompute, sconePostCompute, "lasImage"));

        TeeAppProperties graminePreCompute = new TeeAppProperties("gramine-pre-compute", "gramine-pre-compute-fingerprint", "gramine-pre-compute-entrypoint", 1L);
        TeeAppProperties graminePostCompute = new TeeAppProperties("gramine-post-compute", "gramine-post-compute-fingerprint", "gramine-post-compute-entrypoint", 1L);
        teeProperties.add(new GramineServicesProperties(graminePreCompute, graminePostCompute));

        String jsonString = mapper.writeValueAsString(teeProperties);

        List<TeeServicesProperties> deserializedProperties = mapper.readValue(jsonString, new TypeReference<>() {
        });
        assertThat(deserializedProperties).usingRecursiveComparison().isEqualTo(teeProperties);
    }
}
