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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Getter
@AllArgsConstructor
@Entity
@NoArgsConstructor
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
    public Secret(String address, String value) {
        this.address = address;
        this.setValue(value, false);
    }

    public void setValue(String value, boolean isEncryptedValue) {
        this.value = value;
        this.isEncryptedValue = isEncryptedValue;
    }
}

