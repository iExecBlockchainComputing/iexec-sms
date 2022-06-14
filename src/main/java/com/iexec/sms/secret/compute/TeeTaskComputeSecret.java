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

import com.iexec.sms.secret.SecretUtils;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
 * <p>
 * The <b>key</b> parameter must be a String compliant with the following constraints:
 * <ul>
 * <li>For application developers, it must be a positive number, the characters must be digits [0-9].
 * <li>For requesters, it must be a String of at most 64 characters from [0-9A-Za-z-_].
 * </ul>
 */
@Data
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = {"onChainObjectAddress", "fixedSecretOwner", "key"}) })
public class TeeTaskComputeSecret {

    public static final int SECRET_KEY_MIN_LENGTH = 1;
    public static final int SECRET_KEY_MAX_LENGTH = 64;

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
    private String onChainObjectAddress; // Will be empty for a secret belonging to a requester
    @NotNull
    private OnChainObjectType onChainObjectType;
    @NotNull
    private SecretOwnerRole secretOwnerRole;
    @NotNull
    private String fixedSecretOwner; // Will be empty for a secret belonging to an application developer
    @NotNull
    @Size(min = SECRET_KEY_MIN_LENGTH, max = SECRET_KEY_MAX_LENGTH)
    private String key;
    @NotNull
    /*
     * Expected behavior of AES encryption is to not expand the data very much.
     * Final size might be padded to the next block, plus another padding might
     * be necessary for the IV (https://stackoverflow.com/a/93463).
     * In addition to that, it is worth mentioning that current implementation
     * encrypts the input and produces a Base64 result (stored as-is in
     * database) which causes an overhead of ~33%
     * (https://en.wikipedia.org/wiki/Base64).
     * <p>
     * For theses reasons and simplicity purposes, we reserve twice the size
     * of `SECRET_MAX_SIZE` in storage.
     */
    @Column(length = SecretUtils.SECRET_MAX_SIZE * 2)
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
