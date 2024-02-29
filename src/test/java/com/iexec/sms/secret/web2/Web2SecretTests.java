/*
 * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class Web2SecretTests {
    private static final String OWNER_ADDRESS = "ownerAddress";
    private static final String SECRET_ADDRESS = "secretAddress";

    private static final String VALUE = "value";
    private static final String NEW_VALUE = "newValue";
    private static final Web2Secret SECRET = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, VALUE);
    private static final Web2Secret NEW_SECRET = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, NEW_VALUE);

    @Test
    void withValue() {
        Assertions.assertThat(SECRET.withValue(NEW_VALUE))
                .usingRecursiveComparison()
                .isEqualTo(NEW_SECRET);
    }
}
