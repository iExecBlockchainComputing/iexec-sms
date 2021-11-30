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

package com.iexec.sms.utils;

import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.EthAddress;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

class EthereumCredentialsTest {

    @Test
    void generate() throws GeneralSecurityException {
        EthereumCredentials ethereumCredentials = EthereumCredentials.generate();
        assertFalse(ethereumCredentials.isEncrypted());
        assertTrue(BytesUtils.isNonZeroedBytes32(ethereumCredentials.getPrivateKey()));
        String address = ethereumCredentials.getAddress();
        assertTrue(EthAddress.validate(address)
                && BytesUtils.isNonZeroedHexStringWithPrefixAndProperBytesSize(address,
                20)); // non zeroed
    }

    @Test
    void shouldMatchAddressOfGeneratedCredentials() throws GeneralSecurityException {
        EthereumCredentials generatedCredentials = EthereumCredentials.generate();
        String expectedAddress =
                Credentials.create(generatedCredentials.getPrivateKey()).getAddress();
        assertEquals(expectedAddress, generatedCredentials.getAddress());
    }

}