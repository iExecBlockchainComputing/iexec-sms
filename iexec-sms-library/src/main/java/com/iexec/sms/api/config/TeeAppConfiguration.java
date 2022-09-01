package com.iexec.sms.api.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TeeAppConfiguration {
    private String image;
    private String fingerprint;
    private String entrypoint;
    private long heapSize;  // In GB
}
