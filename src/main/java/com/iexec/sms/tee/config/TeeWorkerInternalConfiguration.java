package com.iexec.sms.tee.config;

import com.iexec.common.tee.TeeFramework;
import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.tee.ConditionalOnTeeFramework;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Configuration
@Validated
public class TeeWorkerInternalConfiguration {
    @Bean
    TeeAppProperties preComputeProperties(
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
        return new TeeAppProperties(
                preComputeImage,
                preComputeFingerprint,
                preComputeEntrypoint,
                DataSize.ofGigabytes(preComputeHeapSizeInGB).toBytes()
        );
    }

    @Bean
    TeeAppProperties postComputeProperties(
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
        return new TeeAppProperties(
                postComputeImage,
                postComputeFingerprint,
                postComputeEntrypoint,
                DataSize.ofGigabytes(postComputeHeapSizeInGB).toBytes()
        );
    }

    @Bean
    @ConditionalOnTeeFramework(frameworks = TeeFramework.GRAMINE)
    GramineServicesProperties gramineServicesProperties(
            TeeAppProperties preComputeProperties,
            TeeAppProperties postComputeProperties) {
        return new GramineServicesProperties(preComputeProperties, postComputeProperties);
    }

    @Bean
    @ConditionalOnTeeFramework(frameworks = TeeFramework.SCONE)
    SconeServicesProperties sconeServicesProperties(
            TeeAppProperties preComputeProperties,
            TeeAppProperties postComputeProperties,
            @Value("${tee.scone.las-image}")
            @NotBlank(message = "las image must be provided")
            String lasImage) {
        return new SconeServicesProperties(preComputeProperties, postComputeProperties, lasImage);
    }
}
