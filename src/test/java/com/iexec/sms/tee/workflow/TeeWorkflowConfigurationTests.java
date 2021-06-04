package com.iexec.sms.tee.workflow;

import com.iexec.common.tee.TeeWorkflowSharedConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

public class TeeWorkflowConfigurationTests {

    private static final String LAS_IMAGE = "lasImage";
    private static final String PRE_COMPUTE_IMAGE = "preComputeImage";
    private static final String PRE_COMPUTE_FINGERPRINT = "preComputeFingerprint";
    private static final int PRE_COMPUTE_HEAP_GB = 1;
    private static final String POST_COMPUTE_IMAGE = "postComputeImage";
    private static final String POST_COMPUTE_FINGERPRINT = "postComputeFingerprint";
    private static final int POST_COMPUTE_HEAP_GB = 2;

    TeeWorkflowConfiguration teeWorkflowConfiguration = new TeeWorkflowConfiguration(null);
    
    @BeforeEach
    void beforeEach() {
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "lasImage", LAS_IMAGE);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "preComputeImage", PRE_COMPUTE_IMAGE);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "preComputeFingerprint", PRE_COMPUTE_FINGERPRINT);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "preComputeHeapSizeGb", PRE_COMPUTE_HEAP_GB);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "postComputeImage", POST_COMPUTE_IMAGE);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "postComputeFingerprint", POST_COMPUTE_FINGERPRINT);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "postComputeHeapSizeGb", POST_COMPUTE_HEAP_GB);
    }

    @Test
    public void shouldGetPublicConfiguration() {
        assertThat(teeWorkflowConfiguration.getSharedConfiguration())
                .isEqualTo(TeeWorkflowSharedConfiguration.builder()
                .lasImage(LAS_IMAGE)
                .preComputeImage(PRE_COMPUTE_IMAGE)
                .preComputeHeapSize(DataSize.ofGigabytes(PRE_COMPUTE_HEAP_GB).toBytes())
                .postComputeImage(POST_COMPUTE_IMAGE)
                .postComputeHeapSize(DataSize.ofGigabytes(POST_COMPUTE_HEAP_GB).toBytes())
                .build());
    }
}
