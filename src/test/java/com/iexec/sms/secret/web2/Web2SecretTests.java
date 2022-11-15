/*
 *
 *  * Copyright 2022 IEXEC BLOCKCHAIN TECH
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.iexec.sms.secret.web2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Web2SecretTests {
    private static final String ID = "id";
    private static final String OWNER_ADDRESS = "ownerAddress";
    private static final String SECRET_ADDRESS = "secretAddress";

    private static final String UNENCRYPTED_VALUE = "unencrypted value";
    private static final String ENCRYPTED_VALUE   = "encrypted value";
    private static final Web2Secret UNENCRYPTED_SECRET = new Web2Secret(ID, OWNER_ADDRESS, SECRET_ADDRESS, UNENCRYPTED_VALUE, false);
    private static final Web2Secret ENCRYPTED_SECRET   = new Web2Secret(ID, OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_VALUE, true);

    @Test
    void withEncryptedValue() {
        assertEquals(ENCRYPTED_SECRET, UNENCRYPTED_SECRET.withEncryptedValue(ENCRYPTED_VALUE));
        assertEquals(ENCRYPTED_SECRET, ENCRYPTED_SECRET.withEncryptedValue(ENCRYPTED_VALUE));
    }

    @Test
    void withDecryptedValue() {
        assertEquals(UNENCRYPTED_SECRET, ENCRYPTED_SECRET.withDecryptedValue(UNENCRYPTED_VALUE));
        assertEquals(UNENCRYPTED_SECRET, UNENCRYPTED_SECRET.withDecryptedValue(UNENCRYPTED_VALUE));
    }
}
