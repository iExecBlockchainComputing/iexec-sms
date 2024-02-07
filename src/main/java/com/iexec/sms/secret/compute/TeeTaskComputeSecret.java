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
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

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
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeeTaskComputeSecret implements Serializable {

    @NotNull
    @EmbeddedId
    private TeeTaskComputeSecretHeader header;

    /*
     * Expected behavior of AES encryption is to not expand the data very much.
     * Final size might be padded to the next block, plus another padding might
     * be necessary for the IV (https://stackoverflow.com/a/93463).
     * In addition to that, it is worth mentioning that current implementation
     * encrypts the input and produces a Base64 result (stored as-is in
     * database) which causes an overhead of ~33%
     * (https://en.wikipedia.org/wiki/Base64).
     * <p>
     * For these reasons and for simplicity purposes, we reserve twice the size
     * of `SECRET_MAX_SIZE` in storage.
     */
    @NotNull
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
        this.header = new TeeTaskComputeSecretHeader(
                onChainObjectType,
                onChainObjectAddress,
                secretOwnerRole,
                fixedSecretOwner,
                key
        );
        this.value = value;
    }

    public TeeTaskComputeSecret withValue(String newValue) {
        return new TeeTaskComputeSecret(header, newValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TeeTaskComputeSecret that = (TeeTaskComputeSecret) o;
        return Objects.equals(header, that.header)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, value);
    }
}
