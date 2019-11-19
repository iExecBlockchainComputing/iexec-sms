package com.iexec.sms.iexecsms.secret;

import com.iexec.common.utils.HashUtils;
import com.iexec.sms.iexecsms.encryption.EncryptionService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Secret {

    private String address; //0xdataset1, aws.amazon.com, beneficiary.key.iex.ec (Kb)
    private String value;
    private boolean isEncryptedValue;

    /* Clear secrets at construction */
    public Secret(String address, String value) {
        this.address = address;
        this.setValue(value, false);
    }

    public void setValue(String value, boolean isEncryptedValue) {
        this.value = value;
        this.isEncryptedValue = isEncryptedValue;
    }

    public void encryptValue(EncryptionService encryptionService) {
        if (!isEncryptedValue) {
            this.setValue(encryptionService.encrypt(value), true);
        }
    }

    public void decryptValue(EncryptionService encryptionService) {
        if (isEncryptedValue) {
            this.setValue(encryptionService.decrypt(value), false);
        }
    }

    public String getHash() {
        return HashUtils.concatenateAndHash(
                HashUtils.sha256(address),
                HashUtils.sha256(value)
        );
    }

    /* private */
    private void setValue(String value) {
        this.value = value;
    }

    /* private */
    private void setEncryptedValue(boolean encryptedValue) {
        isEncryptedValue = encryptedValue;
    }

}
