package com.iexec.sms.tee.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TeeWorkerInternalConfigurationTests {
    private final static String IMAGE = "image";
    private final static String FINGERPRINT = "fingerprint";
    private final static String ENTRYPOINT = "entrypoint";
    private final static String LAS_IMAGE = "lasImage";
    private final static long HEAP_SIZE_GB = 3;
    private final static long HEAP_SIZE_B = 3221225472L;

    private final TeeWorkerInternalConfiguration teeWorkerInternalConfiguration =
            new TeeWorkerInternalConfiguration();

    // region preComputeProperties
    @Test
    void preComputeProperties() {
        TeeAppProperties properties = teeWorkerInternalConfiguration.preComputeProperties(
                IMAGE,
                FINGERPRINT,
                ENTRYPOINT,
                HEAP_SIZE_GB
        );

        assertEquals(IMAGE, properties.getImage());
        assertEquals(FINGERPRINT, properties.getFingerprint());
        assertEquals(ENTRYPOINT, properties.getEntrypoint());
        assertEquals(HEAP_SIZE_B, properties.getHeapSizeInBytes());
    }
    // endregion

    // region postComputeProperties
    @Test
    void postComputeProperties() {
        TeeAppProperties properties = teeWorkerInternalConfiguration.postComputeProperties(
                IMAGE,
                FINGERPRINT,
                ENTRYPOINT,
                HEAP_SIZE_GB
        );

        assertEquals(IMAGE, properties.getImage());
        assertEquals(FINGERPRINT, properties.getFingerprint());
        assertEquals(ENTRYPOINT, properties.getEntrypoint());
        assertEquals(HEAP_SIZE_B, properties.getHeapSizeInBytes());
    }
    // endregion

    // region gramineServicesProperties
    @Test
    void gramineServicesProperties() {
        TeeAppProperties preComputeProperties = mock(TeeAppProperties.class);
        TeeAppProperties postComputeProperties = mock(TeeAppProperties.class);

        GramineServicesProperties properties =
                teeWorkerInternalConfiguration.gramineServicesProperties(preComputeProperties, postComputeProperties);

        assertEquals(TeeEnclaveProvider.GRAMINE, properties.getTeeEnclaveProvider());
        assertEquals(preComputeProperties, properties.getPreComputeProperties());
        assertEquals(postComputeProperties, properties.getPostComputeProperties());
    }
    // endregion

    // region sconeServicesProperties
    @Test
    void sconeServicesProperties() {
        TeeAppProperties preComputeProperties = mock(TeeAppProperties.class);
        TeeAppProperties postComputeProperties = mock(TeeAppProperties.class);

        SconeServicesProperties properties =
                teeWorkerInternalConfiguration.sconeServicesProperties(preComputeProperties, postComputeProperties, LAS_IMAGE);

        assertEquals(TeeEnclaveProvider.SCONE, properties.getTeeEnclaveProvider());
        assertEquals(preComputeProperties, properties.getPreComputeProperties());
        assertEquals(postComputeProperties, properties.getPostComputeProperties());
        assertEquals(LAS_IMAGE, properties.getLasImage());
    }
    // endregion
}