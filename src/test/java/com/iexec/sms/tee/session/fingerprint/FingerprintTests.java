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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FingerprintTests {

    @Test
    public void shouldCreateFingerprint() {
        String fingerprint = "a|b|c|d";
        String[] parts = fingerprint.split("\\|");
        AppFingerprint appFingerprint = new AppFingerprint(fingerprint);
        assertThat(appFingerprint.getFspfKey()).isEqualTo(parts[0]);
        assertThat(appFingerprint.getFspfTag()).isEqualTo(parts[1]);
        assertThat(appFingerprint.getMrEnclave()).isEqualTo(parts[2]);
        assertThat(appFingerprint.getEntrypoint()).isEqualTo(parts[3]);
    }

    @Test
    public void shouldThrowWhenNullFingerprint() {
        assertThrows(IllegalArgumentException.class,
                () -> new PreComputeFingerprint(null));
    }

    @Test
    public void shouldThrowWhenInvalidFingerprint() {
        String badFingerprint = "a|b|";
        assertThrows(IllegalArgumentException.class,
                () -> new PostComputeFingerprint(badFingerprint));
    }
}
