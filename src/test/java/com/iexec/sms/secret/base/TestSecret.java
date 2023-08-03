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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import static com.iexec.sms.secret.base.TestSecretHeader.TEST_SECRET_HEADER;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TestSecret extends AbstractSecret<TestSecret, TestSecretHeader> {
    public static final String PLAIN_SECRET_VALUE = "plainSecretValue";
    public static final String ENCRYPTED_SECRET_VALUE = "encryptedSecretValue";

    @Transient
    public static final TestSecret TEST_SECRET = new TestSecret(TEST_SECRET_HEADER, ENCRYPTED_SECRET_VALUE);

    @NotNull
    @EmbeddedId
    private TestSecretHeader header;

    public TestSecret(TestSecretHeader header, String value) {
        super(value);
        this.header = header;
    }

    @Override
    protected TestSecret withValue(String newValue) {
        return new TestSecret(header, newValue);
    }
}
