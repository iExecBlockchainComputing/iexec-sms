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
