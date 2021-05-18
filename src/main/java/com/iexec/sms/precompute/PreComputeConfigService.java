package com.iexec.sms.precompute;

import com.iexec.common.precompute.PreComputeConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class PreComputeConfigService {

    private final PreComputeConfig configuration;

    public PreComputeConfigService(
            @Value("${pre-compute.image}") String image,
            @Value("${pre-compute.fingerprint}") String fingerprint,
            @Value("${pre-compute.heap-size}") String heapSize) {
        if (StringUtils.isEmpty(image)) {
            throw new IllegalStateException("No pre-compute image is provided");
        }
        if (StringUtils.isEmpty(fingerprint)) {
            throw new IllegalStateException("No pre-compute fingerprint is provided");
        }
        if (StringUtils.isEmpty(heapSize)) {
            throw new IllegalStateException("No pre-compute heap size is provided");
        }
        configuration = new PreComputeConfig(image, fingerprint, heapSize);
    }
}
