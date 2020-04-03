package com.iexec.sms.iexecsms.tee.session.fingerprint;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FingerprintUtils {

    public static DatasetFingerprint toDatasetFingerprint(String fingerprint) {
        Optional<List<String>> oParts = getFingerprintParts(fingerprint, 2);
        if (!oParts.isPresent()) {
            return null;
        }
        List<String> parts = oParts.get();

        return DatasetFingerprint.builder()
                .fspfKey(parts.get(0))
                .fspfTag(parts.get(1))
                .build();
    }

    public static PostComputeFingerprint toPostComputeFingerprint(String fingerprint) {
        Optional<List<String>> oParts = getFingerprintParts(fingerprint, 3);
        if (oParts.isEmpty()) {
            return null;
        }
        List<String> parts = oParts.get();

        return PostComputeFingerprint.builder()
                .fspfKey(parts.get(0))
                .fspfTag(parts.get(1))
                .mrEnclave(parts.get(2))
                .build();
    }

    public static AppFingerprint toAppFingerprint(String fingerprint) {
        Optional<List<String>> oParts = getFingerprintParts(fingerprint, 4);
        if (oParts.isEmpty()) {
            return null;
        }
        List<String> parts = oParts.get();

        return AppFingerprint.builder()
                .fspfKey(parts.get(0))
                .fspfTag(parts.get(1))
                .mrEnclave(parts.get(2))
                .entrypoint(parts.get(3))
                .build();
    }

    private static Optional<List<String>> getFingerprintParts(String fingerprint, int expectedParts) {
        String[] fingerprintParts = fingerprint.split("\\|");

        if (fingerprintParts.length < expectedParts) {
            return Optional.empty();
        }

        return Optional.of(Arrays.asList(fingerprintParts));
    }


}