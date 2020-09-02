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

import java.math.BigInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
@Entity
public class EthereumCredentials {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String address;
    private String privateKey;
    private String publicKey;
    private boolean isEncrypted; // private & public keys

    public EthereumCredentials() throws Exception {
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        this.address = Numeric.prependHexPrefix(Keys.getAddress(ecKeyPair));
        setPlainKeys(toHex(ecKeyPair.getPrivateKey()),
                toHex(ecKeyPair.getPublicKey()));
    }

    public void setPlainKeys(String privateKey, String publicKey) {
        this.setKeys(privateKey, publicKey, false);
    }

    public void setEncryptedKeys(String privateKey, String publicKey) {
        this.setKeys(privateKey, publicKey, true);
    }

    private void setKeys(String privateKey, String publicKey, boolean isEncrypted) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.isEncrypted = isEncrypted;
    }

    private String toHex(BigInteger input) {
        return Numeric.prependHexPrefix(input.toString(16));
    }
}
