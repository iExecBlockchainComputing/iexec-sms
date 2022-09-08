package com.iexec.sms.tee.config;

import com.iexec.sms.api.config.TeeAppConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Configuration
public class TeeWorkerInternalConfiguration {
    @Bean
    TeeAppConfiguration preComputeConfiguration(
            @Value("${tee.worker.pre-compute.image}")
            @NotBlank(message = "pre-compute image must be provided")
            String preComputeImage,
            @Value("${tee.worker.pre-compute.fingerprint}")
            @NotBlank(message = "pre-compute fingerprint must be provided")
            String preComputeFingerprint,
            @Value("${tee.worker.pre-compute.entrypoint}")
            @NotBlank(message = "pre-compute entrypoint must be provided")
            String preComputeEntrypoint,
            @Value("${tee.worker.pre-compute.heap-size-gb}")
            @Positive(message = "pre-compute heap size must be provided")
            long preComputeHeapSizeInGB) {
        return new TeeAppConfiguration(
                preComputeImage,
                preComputeFingerprint,
                preComputeEntrypoint,
                DataSize.ofGigabytes(preComputeHeapSizeInGB).toBytes()
        );
    }

    @Bean
    TeeAppConfiguration postComputeConfiguration(
            @Value("${tee.worker.post-compute.image}")
            @NotBlank(message = "post-compute image must be provided")
            String postComputeImage,
            @Value("${tee.worker.post-compute.fingerprint}")
            @NotBlank(message = "post-compute fingerprint must be provided")
            String postComputeFingerprint,
            @Value("${tee.worker.post-compute.entrypoint}")
            @NotBlank(message = "post-compute entrypoint must be provided")
            String postComputeEntrypoint,
            @Value("${tee.worker.post-compute.heap-size-gb}")
            @Positive(message = "post-compute heap size must be provided")
            long postComputeHeapSizeInGB) {
        return new TeeAppConfiguration(
                postComputeImage,
                postComputeFingerprint,
                postComputeEntrypoint,
                DataSize.ofGigabytes(postComputeHeapSizeInGB).toBytes()
        );
    }

}
