/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.challenge;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.GeneralSecurityException;
import java.time.Instant;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeeChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String taskId;

    private Instant finalDeadline;

    @OneToOne(cascade = {CascadeType.ALL})
    private EthereumCredentials credentials;

    public TeeChallenge(final String taskId, final Instant finalDeadline) throws GeneralSecurityException {
        this.taskId = taskId;
        this.credentials = EthereumCredentials.generate();
        this.finalDeadline = finalDeadline;
    }
}
