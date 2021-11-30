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

package com.iexec.sms.utils;

import com.iexec.common.utils.CredentialsUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.hibernate.annotations.GenericGenerator;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Domain entity
 */
@Data
@Getter
@AllArgsConstructor
@Entity
public class EthereumCredentials {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String privateKey;
    private boolean isEncrypted;

    private EthereumCredentials(String privateKey) {
        this.setPlainTextPrivateKey(privateKey);
    }

    /**
     * Build EthereumCredentials from a random private key (generated from a
     * secure random source).
     *
     * @return Ethereum credentials
     * @throws java.security.GeneralSecurityException exception if failed to
     *                                                generate credentials
     */
    public static EthereumCredentials generate() throws java.security.GeneralSecurityException {
        ECKeyPair randomEcKeyPair = Keys.createEcKeyPair();
        String privateKey =
                Numeric.toHexStringWithPrefixZeroPadded(randomEcKeyPair.getPrivateKey(),
                        Keys.PRIVATE_KEY_LENGTH_IN_HEX);//hex-string size of 32 bytes (64)
        return new EthereumCredentials(privateKey);
    }

    public String getAddress() {
        return isEncrypted ? "" : CredentialsUtils.getAddress(privateKey);
    }

    public void setPlainTextPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        this.isEncrypted = false;
    }

    public void setEncryptedPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        this.isEncrypted = true;
    }

}
