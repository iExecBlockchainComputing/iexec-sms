/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.compute;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Define a secret that can be used during the execution of a TEE task.
 * Currently, only secrets for application developers and requesters are supported.
 * <p>
 * In this implementation, a unique constraint has been added on <b>onChainObjectAddress</b>,
 * <b>fixedSecretOwner</b> and <b>key</b> columns.
 * This constraint has been defined in such a way because:
 * <ul>
 * <li>For application developers, fixedSecretOwner will always be "". Each application developer
 *     secret is uniquely identified with onChainObjectAddress and key (long parsed as String) values.
 * <li>For requesters, onChainObjectAddress will always be "". Each requester secret is uniquely
 *     identified with fixedSecretOwner and key values.
 * </ul>
 */
@Data
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = {"onChainObjectAddress", "fixedSecretOwner", "key"}) })
public class TeeTaskComputeSecret {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    /**
     * Represents the blockchain address of the deployed object
     * (0xapplication, 0xdataset, 0xworkerpool)
     * <p>
     * In a future release, it should also handle ENS names.
     */
    @NotNull
    private String onChainObjectAddress;
    @NotNull
    private OnChainObjectType onChainObjectType;
    @NotNull
    private SecretOwnerRole secretOwnerRole;
    @NotNull
    private String fixedSecretOwner;  // May be empty if the owner is not fixed
    @NotNull
    private String key;
    @NotNull
    private String value;

    @Builder
    public TeeTaskComputeSecret(
            OnChainObjectType onChainObjectType,
            String onChainObjectAddress,
            SecretOwnerRole secretOwnerRole,
            String fixedSecretOwner,
            String key,
            String value) {
        this.onChainObjectType = onChainObjectType;
        this.onChainObjectAddress = onChainObjectAddress;
        this.secretOwnerRole = secretOwnerRole;
        this.fixedSecretOwner = fixedSecretOwner;
        this.key = key;
        this.value = value;
    }
}
