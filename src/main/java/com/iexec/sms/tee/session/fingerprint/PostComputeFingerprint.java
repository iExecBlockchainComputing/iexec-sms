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

import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostComputeFingerprint extends SconeFingerprint {

    private String fspfKey;
    private String fspfTag;
    private String mrEnclave;

    public PostComputeFingerprint(String fingerprint) {
        List<String> parts = getFingerprintParts(fingerprint, 3);
        if (parts.isEmpty()) {
            throw new IllegalStateException("Invalid post-compute fingerprint: " +
                    fingerprint);
        }
        this.fspfKey = parts.get(0);
        this.fspfTag = parts.get(1);
        this.mrEnclave = parts.get(2);
    }
}