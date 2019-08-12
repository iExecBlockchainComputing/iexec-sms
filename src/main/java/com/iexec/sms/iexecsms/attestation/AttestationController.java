package com.iexec.sms.iexecsms.attestation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
public class AttestationController {

    private AttestationService attestationService;

    public AttestationController(AttestationService attestationService) {
        this.attestationService = attestationService;
    }

    /**
     * Called by the core, not the worker
     */
    @PostMapping("/attestations/generate/{taskId}")
    public ResponseEntity<Attestation> generateAttestation(@RequestParam String taskId) {
        Optional<Attestation> oAttestation = attestationService.getOrCreate(taskId);

        if (oAttestation.isPresent()) {
            return ResponseEntity.ok(oAttestation.get());
        }
        return ResponseEntity.notFound().build();
    }
}
