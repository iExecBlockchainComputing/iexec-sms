package com.iexec.sms.precompute;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/precompute")
public class PreComputeController {

    private final PreComputeConfig preComputeConfig;

    public PreComputeController(PreComputeConfig preComputeConfig) {
        this.preComputeConfig = preComputeConfig;
    }

    @GetMapping("/image")
    public ResponseEntity<String> getPreComputeImageUri() {
        return ResponseEntity.ok(preComputeConfig.getImage());
    }
}
