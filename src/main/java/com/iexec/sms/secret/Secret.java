package com.iexec.sms.secret;

import com.iexec.sms.encryption.EncryptionService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Getter
@AllArgsConstructor
@Entity
@NoArgsConstructor
public class Secret {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String address; //0xdataset1, aws.amazon.com, beneficiary.key.iex.ec (Kb)
    @Column(columnDefinition = "LONGTEXT")
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
}

