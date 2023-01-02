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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Secret {
    @Column(columnDefinition = "LONGTEXT")
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
    protected Secret(String value) {
        this.value = value != null ? value.trim() : null;
    }
}

