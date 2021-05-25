package com.iexec.sms.tee.workflow;

import com.iexec.common.tee.TeeWorkflowSharedConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TeeWorkflowConfigurationTests {

    private static final String PRE_COMPUTE_IMAGE = "preComputeImage";
    private static final String PRE_COMPUTE_FINGERPRINT = "preComputeFingerprint";
    private static final String PRE_COMPUTE_HEAP = "preComputeHeap";
    private static final String POST_COMPUTE_IMAGE = "postComputeImage";
    private static final String POST_COMPUTE_FINGERPRINT = "postComputeFingerprint";
    private static final String POST_COMPUTE_HEAP = "postComputeHeap";

    TeeWorkflowConfiguration teeWorkflowConfiguration = new TeeWorkflowConfiguration(
            PRE_COMPUTE_IMAGE,
            PRE_COMPUTE_FINGERPRINT,
            PRE_COMPUTE_HEAP,
            POST_COMPUTE_IMAGE,
            POST_COMPUTE_FINGERPRINT,
            POST_COMPUTE_HEAP);


    @Test
    public void shouldGetPublicConfiguration() {
        assertThat(teeWorkflowConfiguration.getPublicConfiguration())
                .isEqualTo(TeeWorkflowSharedConfiguration.builder()
                .preComputeImage(PRE_COMPUTE_IMAGE)
                .preComputeHeapSize(PRE_COMPUTE_HEAP)
                .postComputeImage(POST_COMPUTE_IMAGE)
                .postComputeHeapSize(POST_COMPUTE_HEAP)
                .build());
    }
}
