package com.iexec.sms.tee.workflow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TeeWorkflowInternalConfigurationTests {

    @Test
    void testGetSharedConfigWithHeapSizeValues() {
        var teeWorkflowInternalConfiguration = new TeeWorkflowInternalConfiguration(
                "lasImage", "preImage",
                "preFingerprint", "preEntrypoint", 1, "postImage",
                "postFingerprint", "postEntrypoint", 2, null);
        Assertions.assertThat(teeWorkflowInternalConfiguration.getSharedConfig()
                .getPreComputeHeapSize()).isEqualTo(1073741824L);
        Assertions.assertThat(teeWorkflowInternalConfiguration.getSharedConfig()
                .getPostComputeHeapSize()).isEqualTo(2147483648L);
    }

}
