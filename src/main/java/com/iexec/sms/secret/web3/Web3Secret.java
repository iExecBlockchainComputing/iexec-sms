package com.iexec.sms.secret.web3;

import com.iexec.sms.secret.Secret;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@EqualsAndHashCode(callSuper = true)
@Data
@Getter
@Entity
@NoArgsConstructor
public class Web3Secret extends Secret {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    Web3Secret(String address, String value) {
        super(address, value);
    }
}
