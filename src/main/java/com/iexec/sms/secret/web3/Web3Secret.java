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

package com.iexec.sms.secret.web3;

import com.iexec.sms.secret.Secret;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Web3Secret extends Secret {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private Web3Secret(String superId, String id, String address, String value, boolean isEncryptedValue) {
        super(superId, address, value, isEncryptedValue);
        this.id = id;
    }

    public Web3Secret(String address, String value, boolean isEncryptedValue) {
        super(address, value, isEncryptedValue);
    }

    /**
     * Copies the current {@link Web3Secret} object,
     * while replacing the old value with the new one.
     *
     * @param newValue         Value to use for new object.
     * @param isEncryptedValue Whether this value is encrypted.
     * @return A new {@link Web3Secret} object with new value.
     */
    @Override
    public Web3Secret withValue(String newValue, boolean isEncryptedValue) {
        return new Web3Secret(super.getId(), this.id, this.getAddress(), newValue, isEncryptedValue);
    }
}
