package com.iexec.sms.precompute;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PreComputeConfig {

    @Getter
    @Value("${pre-compute.image}")
    String image;

    @Getter
    @Value("${pre-compute.fingerprint}")
    String fingerprint;
}
