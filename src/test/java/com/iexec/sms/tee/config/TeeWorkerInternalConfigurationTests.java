/*
 * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeeWorkerInternalConfigurationTests {
    private static final String IMAGE = "image";
    private static final String FINGERPRINT = "fingerprint";
    private static final String ENTRYPOINT = "entrypoint";
    private static final String LAS_IMAGE = "lasImage";
    private static final long HEAP_SIZE_GB = 3;
    private static final long HEAP_SIZE_B = 3221225472L;

    private final TeeAppProperties preComputeProperties = TeeAppProperties.builder()
            .image("preComputeImage")
            .fingerprint("preComputeFingerprint")
            .entrypoint("preComputeEntrypoint")
            .heapSizeInBytes(1L)
            .build();
    private final TeeAppProperties postComputeProperties = TeeAppProperties.builder()
            .image("postComputeImage")
            .fingerprint("postComputeFingerprint")
            .entrypoint("postComputeEntrypoint")
            .heapSizeInBytes(1L)
            .build();


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
        GramineServicesProperties properties =
                teeWorkerInternalConfiguration.gramineServicesProperties(preComputeProperties, postComputeProperties);

        assertEquals(TeeFramework.GRAMINE, properties.getTeeFramework());
        assertEquals(preComputeProperties, properties.getPreComputeProperties());
        assertEquals(postComputeProperties, properties.getPostComputeProperties());
    }
    // endregion

    // region sconeServicesProperties
    @Test
    void sconeServicesProperties() {
        SconeServicesProperties properties =
                teeWorkerInternalConfiguration.sconeServicesProperties(preComputeProperties, postComputeProperties, LAS_IMAGE);

        assertEquals(TeeFramework.SCONE, properties.getTeeFramework());
        assertEquals(preComputeProperties, properties.getPreComputeProperties());
        assertEquals(postComputeProperties, properties.getPostComputeProperties());
        assertEquals(LAS_IMAGE, properties.getLasImage());
    }
    // endregion
}