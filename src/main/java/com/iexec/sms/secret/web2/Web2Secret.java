/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.web2;

import com.iexec.sms.secret.Secret;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Web2Secret extends Secret {
    @EmbeddedId
    private Web2SecretHeader header;

    public Web2Secret(String ownerAddress, String address, String value) {
        this(new Web2SecretHeader(ownerAddress, address), value);
    }

    private Web2Secret(Web2SecretHeader header, String value) {
        super(value);
        this.header = header;
    }

    /**
     * Copies the current {@link Web2Secret} object,
     * while replacing the old value with a new value.
     *
     * @param newValue Value to use for new object.
     * @return A new {@link Web2Secret} object with new value.
     */
    public Web2Secret withValue(String newValue) {
        return new Web2Secret(header, newValue);
    }
}
