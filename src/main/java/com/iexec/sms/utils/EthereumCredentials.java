package com.iexec.sms.utils;

import com.iexec.sms.encryption.EncryptionService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigInteger;

@Data
@Getter
@AllArgsConstructor
@Entity
@NoArgsConstructor
public class EthereumCredentials {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String address;
    private String privateKey;
    private String publicKey;
    private boolean areEncryptedKeys;

    public EthereumCredentials(ECKeyPair ecKeyPair) {
        this.address = Numeric.prependHexPrefix(Keys.getAddress(ecKeyPair));
        setKeys(toHex(ecKeyPair.getPrivateKey()),
                toHex(ecKeyPair.getPublicKey()),
                false);
    }

    private String toHex(BigInteger input) {
        return Numeric.prependHexPrefix(input.toString(16));
    }

    private void setKeys(String privateKey, String publicKey, boolean areEncryptedKeys) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.areEncryptedKeys = areEncryptedKeys;
    }

    public void encryptKeys(EncryptionService encryptionService) {
        if (!areEncryptedKeys) {
            this.setKeys(encryptionService.encrypt(privateKey),
                    encryptionService.encrypt(publicKey),
                    true);
        }
    }

    public void decryptKeys(EncryptionService encryptionService) {
        if (areEncryptedKeys) {
            this.setKeys(encryptionService.decrypt(privateKey),
                    encryptionService.decrypt(publicKey),
                    false);
        }
    }

}


