/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.base;

import com.iexec.sms.secret.SecretUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractSecret<S extends AbstractSecret<S, H>, H extends AbstractSecretHeader> implements Serializable {
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

    /**
     * Create the secret without possible leading or trailing
     * newline characters. This should be used when putting
     * the secret in the palaemon session. We decided to handle
     * this specific case because it has a good probability to occur
     * (when reading the secret from a file and uploading it to the
     * SMS without any trimming) and it can break the workflow even
     * though everything is correctly setup.
     */
    protected AbstractSecret(String value) {
        Objects.requireNonNull(value, "Secret value must not be null");
        this.value = value.trim();
    }

    protected abstract S withValue(String newValue);
}

