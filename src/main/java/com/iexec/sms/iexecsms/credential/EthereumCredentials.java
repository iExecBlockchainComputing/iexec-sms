package com.iexec.sms.iexecsms.credential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EthereumCredentials {

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


