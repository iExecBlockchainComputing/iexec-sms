package com.iexec.sms.tee.config;

import com.iexec.sms.api.config.TeeAppConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Configuration
public class SideComputeInternalConfiguration {
    @Bean
    TeeAppConfiguration preComputeConfiguration(
            @Value("${tee.workflow.pre-compute.image}")
            @NotBlank(message = "pre-compute image must be provided")
            String preComputeImage,
            @Value("${tee.workflow.pre-compute.fingerprint}")
            @NotBlank(message = "pre-compute fingerprint must be provided")
            String preComputeFingerprint,
            @Value("${tee.workflow.pre-compute.entrypoint}")
            @NotBlank(message = "pre-compute entrypoint must be provided")
            String preComputeEntrypoint,
            @Value("${tee.workflow.pre-compute.heap-size-gb}")
            @Positive(message = "pre-compute heap size must be provided")
            long preComputeHeapSize) {
        return new TeeAppConfiguration(
                preComputeImage,
                preComputeFingerprint,
                preComputeEntrypoint,
                DataSize.ofGigabytes(preComputeHeapSize).toBytes()
        );
    }

    @Bean
    TeeAppConfiguration postComputeConfiguration(
            @Value("${tee.workflow.post-compute.image}")
            @NotBlank(message = "post-compute image must be provided")
            String postComputeImage,
            @Value("${tee.workflow.post-compute.fingerprint}")
            @NotBlank(message = "post-compute fingerprint must be provided")
            String postComputeFingerprint,
            @Value("${tee.workflow.post-compute.entrypoint}")
            @NotBlank(message = "post-compute entrypoint must be provided")
            String postComputeEntrypoint,
            @Value("${tee.workflow.post-compute.heap-size-gb}")
            @Positive(message = "post-compute heap size must be provided")
            long postComputeHeapSize) {
        return new TeeAppConfiguration(
                postComputeImage,
                postComputeFingerprint,
                postComputeEntrypoint,
                DataSize.ofGigabytes(postComputeHeapSize).toBytes()
        );
    }

}
