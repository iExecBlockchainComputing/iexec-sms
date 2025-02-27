/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.challenge;

import com.iexec.commons.poco.utils.CredentialsUtils;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.security.GeneralSecurityException;

/**
 * Domain entity
 */
@Entity
@Getter
@NoArgsConstructor //for hibernate
@AllArgsConstructor
public class EthereumCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String privateKey;
    private boolean isEncrypted;
    /*
     * Address is required since recovering from private key is not possible
     * from an encrypted private key state.
     */
    private String address;

    private EthereumCredentials(String privateKey, String address) {
        this.setPlainTextPrivateKey(privateKey);
        this.address = address;
    }

    /**
     * Build EthereumCredentials from a random private key (generated from a
     * secure random source).
     *
     * @return Ethereum credentials
     * @throws GeneralSecurityException exception if credentials generation failed
     */
    public static EthereumCredentials generate() throws GeneralSecurityException {
        ECKeyPair randomEcKeyPair = Keys.createEcKeyPair();
        String privateKey =
                Numeric.toHexStringWithPrefixZeroPadded(randomEcKeyPair.getPrivateKey(),
                        Keys.PRIVATE_KEY_LENGTH_IN_HEX);//hex-string size of 32 bytes (64)
        return new EthereumCredentials(privateKey, CredentialsUtils.getAddress(privateKey));
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
