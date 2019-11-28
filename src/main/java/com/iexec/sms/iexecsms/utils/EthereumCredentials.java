package com.iexec.sms.iexecsms.utils;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class EthereumCredentials {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String address;
    private String privateKey;
    private String publicKey;

    public EthereumCredentials(ECKeyPair ecKeyPair) {
        this.address = Numeric.prependHexPrefix(Keys.getAddress(ecKeyPair));
        this.privateKey = toHex(ecKeyPair.getPrivateKey());
        this.publicKey = toHex(ecKeyPair.getPublicKey());

    }

    private String toHex(BigInteger input) {
        return Numeric.prependHexPrefix(input.toString(16));
    }

}


