package com.iexec.sms.precompute;

import com.iexec.common.precompute.PreComputeConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/precompute")
public class PreComputeController {

    private final PreComputeConfigService preComputeConfigService;

    public PreComputeController(PreComputeConfigService preComputeConfigService) {
        this.preComputeConfigService = preComputeConfigService;
    }

    /**
     * Retrieve configuration for running pre-compute stage.
     * <p>
     * Note: Being able to read the fingerprint on this endpoint is not required
     * for the workflow but it might be convenient to keep it for
     * transparency purposes.
     *
     * @return pre-compute config (image uri, fingerprint, heap size)
     */
    @GetMapping("/config")
    public ResponseEntity<PreComputeConfig> getPreComputeConfig() {
        return ResponseEntity.ok(preComputeConfigService.getConfiguration());
    }
}
