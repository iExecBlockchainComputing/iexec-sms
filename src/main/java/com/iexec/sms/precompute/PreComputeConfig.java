package com.iexec.sms.precompute;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class PreComputeConfig {

    String image;
    String fingerprint;

    public PreComputeConfig(
            @Value("${pre-compute.image}") String image,
            @Value("${pre-compute.fingerprint}") String fingerprint) {
        
        if (StringUtils.isEmpty(image)) {
            throw new IllegalStateException("No pre-compute image is provided");
        }
        if (StringUtils.isEmpty(fingerprint)) {
            throw new IllegalStateException("No pre-compute fingerprint is provided");
        }
        this.image = image;
        this.fingerprint = fingerprint;
    }
}
