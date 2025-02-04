package com.iexec.sms.api.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeeServicesPropertiesTests {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String FRAMEWORK_VERSION = "v5";

    private static final String SCONE_PRE_COMPUTE_NAME = "scone-pre-compute";
    private static final String SCONE_POST_COMPUTE_NAME = "scone-post-compute";
    private static final String GRAMINE_PRE_COMPUTE_NAME = "gramine-pre-compute";
    private static final String GRAMINE_POST_COMPUTE_NAME = "gramine-post-compute";
    private static final String LAS_IMAGE = "lasImage";
    private static final long HEAP_SIZE = 1L;

    private List<TeeServicesProperties> teeProperties;

    @BeforeEach
    void setUp() {
        final TeeAppProperties sconePreCompute = createTeeAppProperties(
                SCONE_PRE_COMPUTE_NAME,
                SCONE_PRE_COMPUTE_NAME + "-fingerprint",
                SCONE_PRE_COMPUTE_NAME + "-entrypoint"
        );
        final TeeAppProperties sconePostCompute = createTeeAppProperties(
                SCONE_POST_COMPUTE_NAME,
                SCONE_POST_COMPUTE_NAME + "-fingerprint",
                SCONE_POST_COMPUTE_NAME + "-entrypoint"
        );
        final TeeAppProperties graminePreCompute = createTeeAppProperties(
                GRAMINE_PRE_COMPUTE_NAME,
                GRAMINE_PRE_COMPUTE_NAME + "-fingerprint",
                GRAMINE_PRE_COMPUTE_NAME + "-entrypoint"
        );
        final TeeAppProperties graminePostCompute = createTeeAppProperties(
                GRAMINE_POST_COMPUTE_NAME,
                GRAMINE_POST_COMPUTE_NAME + "-fingerprint",
                GRAMINE_POST_COMPUTE_NAME + "-entrypoint"
        );

        teeProperties = new ArrayList<>();
        teeProperties.add(new SconeServicesProperties(FRAMEWORK_VERSION, sconePreCompute, sconePostCompute, LAS_IMAGE));
        teeProperties.add(new GramineServicesProperties(FRAMEWORK_VERSION, graminePreCompute, graminePostCompute));
    }

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        final String jsonString = mapper.writeValueAsString(teeProperties);
        final List<TeeServicesProperties> deserializedProperties = mapper.readValue(
                jsonString,
                new TypeReference<>() {
                }
        );

        assertThat(deserializedProperties)
                .usingRecursiveComparison()
                .isEqualTo(teeProperties);
    }

    @Test
    void shouldPreserveFrameworkVersion() throws JsonProcessingException {
        final String jsonString = mapper.writeValueAsString(teeProperties);
        final List<TeeServicesProperties> deserializedProperties = mapper.readValue(
                jsonString,
                new TypeReference<>() {
                }
        );

        assertThat(deserializedProperties.get(0).getTeeFrameworkVersion())
                .isEqualTo(FRAMEWORK_VERSION);
        assertThat(deserializedProperties.get(1).getTeeFrameworkVersion())
                .isEqualTo(FRAMEWORK_VERSION);
    }

    private TeeAppProperties createTeeAppProperties(
            final String name,
            final String fingerprint,
            final String entrypoint
    ) {
        return new TeeAppProperties(name, fingerprint, entrypoint, HEAP_SIZE);
    }
}
