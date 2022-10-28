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

package com.iexec.sms.secret;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Secret {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String address; //0xdataset1, aws.amazon.com, beneficiary.key.iex.ec (Kb)
    @Column(columnDefinition = "LONGTEXT")
    private String value;
    private boolean isEncryptedValue;

    /* Clear secrets at construction */
    public Secret(String address, String value, boolean isEncryptedValue) {
        this.address = address;
        this.value = value;
        this.isEncryptedValue = isEncryptedValue;
    }

    /**
     * Copies the current {@link Secret} object,
     * while replacing the old value with a new encrypted value.
     *
     * @param newValue         Value to use for new object.
     * @return A new {@link Secret} object with new value.
     */
    public Secret withEncryptedValue(String newValue) {
        return new Secret(this.id, this.address, newValue, true);
    }

    /**
     * Copies the current {@link Secret} object,
     * while replacing the old value with a new decrypted value.
     *
     * @param newValue Value to use for new object.
     * @return A new {@link Secret} object with new value.
     */
    public Secret withDecryptedValue(String newValue) {
        return new Secret(this.id, this.address, newValue, false);
    }


    /**
     * Get the secret value without possible leading or trailing
     * newline characters. This should be used when putting
     * the secret in the palaemon session. We decided to handle
     * this specific case because it has a good probability to occur
     * (when reading the secret from a file and uploading it to the
     * SMS without any trimming) and it can break the workflow even
     * though everything is correctly setup.
     * 
     * @return trimmed secret value
     */
    public String getTrimmedValue() {
        Objects.requireNonNull(this.value, "Secret value must not be null");
        return this.value.trim();
    }

    @Override
    public String toString() {
        return "Secret{" +
                "id='" + id + '\'' +
                ", address='" + address + '\'' +
                ", value='" + (isEncryptedValue ? value : "<plain text value>") + '\'' +
                ", isEncryptedValue=" + isEncryptedValue +
                '}';
    }
}

