package com.iexec.sms.tee.session.precompute;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PreComputeConfig {

    @Value("${pre-compute.fingerprint}")
    @Getter
    String fingerprint;
}
