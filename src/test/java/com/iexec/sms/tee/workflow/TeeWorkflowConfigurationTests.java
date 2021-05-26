package com.iexec.sms.tee.workflow;

import com.iexec.common.tee.TeeWorkflowSharedConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class TeeWorkflowConfigurationTests {

    private static final String PRE_COMPUTE_IMAGE = "preComputeImage";
    private static final String PRE_COMPUTE_FINGERPRINT = "preComputeFingerprint";
    private static final long PRE_COMPUTE_HEAP = 1024;
    private static final String POST_COMPUTE_IMAGE = "postComputeImage";
    private static final String POST_COMPUTE_FINGERPRINT = "postComputeFingerprint";
    private static final long POST_COMPUTE_HEAP = 2048;

    TeeWorkflowConfiguration teeWorkflowConfiguration = new TeeWorkflowConfiguration(null);
    
    @BeforeEach
    void beforeEach() {
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "preComputeImage", PRE_COMPUTE_IMAGE);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "preComputeFingerprint", PRE_COMPUTE_FINGERPRINT);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "preComputeHeapSize", PRE_COMPUTE_HEAP);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "postComputeImage", POST_COMPUTE_IMAGE);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "postComputeFingerprint", POST_COMPUTE_FINGERPRINT);
        ReflectionTestUtils.setField(teeWorkflowConfiguration, "postComputeHeapSize", POST_COMPUTE_HEAP);
    }

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
