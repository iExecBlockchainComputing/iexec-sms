/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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