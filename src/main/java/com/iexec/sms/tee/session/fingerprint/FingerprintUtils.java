package com.iexec.sms.tee.session.fingerprint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FingerprintUtils {

    public static DatasetFingerprint toDatasetFingerprint(String fingerprint) {
        List<String> parts = getFingerprintParts(fingerprint, 2);
        if (parts.isEmpty()) {
            return null;
        }

        return DatasetFingerprint.builder()
                .fspfKey(parts.get(0))
                .fspfTag(parts.get(1))
                .build();
    }

    public static PostComputeFingerprint toPostComputeFingerprint(String fingerprint) {
        List<String> parts = getFingerprintParts(fingerprint, 3);
        if (parts.isEmpty()) {
            return null;
        }

        return PostComputeFingerprint.builder()
                .fspfKey(parts.get(0))
                .fspfTag(parts.get(1))
                .mrEnclave(parts.get(2))
                .build();
    }

    public static AppFingerprint toAppFingerprint(String fingerprint) {
        List<String> parts = getFingerprintParts(fingerprint, 4);
        if (parts.isEmpty()) {
            return null;
        }

        return AppFingerprint.builder()
                .fspfKey(parts.get(0))
                .fspfTag(parts.get(1))
                .mrEnclave(parts.get(2))
                .entrypoint(parts.get(3))
                .build();
    }

    private static List<String> getFingerprintParts(String fingerprint, int expectedParts) {
        if (fingerprint == null || fingerprint.isEmpty()) {
            return Collections.emptyList();
        }

        String[] fingerprintParts = fingerprint.split("\\|");

        if (fingerprintParts.length < expectedParts) {
            return Collections.emptyList();
        }

        return Arrays.asList(fingerprintParts);
    }


}