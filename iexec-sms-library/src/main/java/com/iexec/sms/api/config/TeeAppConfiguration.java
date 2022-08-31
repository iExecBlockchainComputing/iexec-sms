package com.iexec.sms.api.config;

import lombok.Getter;

@Getter
public class TeeAppConfiguration {
    private final String image;
    private final String fingerprint;
    private final String entrypoint;
    private final long heapSize;

    public TeeAppConfiguration(String image, String fingerprint, String entrypoint, long heapSize) {
        this.image = image;
        this.fingerprint = fingerprint;
        this.entrypoint = entrypoint;
        this.heapSize = heapSize;
    }
}
