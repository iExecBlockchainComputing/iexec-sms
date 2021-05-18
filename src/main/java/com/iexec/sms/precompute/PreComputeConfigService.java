package com.iexec.sms.precompute;

import com.iexec.common.precompute.PreComputeConfig;
import lombok.Getter;
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
        configuration = new PreComputeConfig(image, fingerprint, heapSize);
    }
}
