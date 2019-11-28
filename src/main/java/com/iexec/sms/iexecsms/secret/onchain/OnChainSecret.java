package com.iexec.sms.iexecsms.secret.onchain;

import com.iexec.sms.iexecsms.secret.Secret;
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
public class OnChainSecret extends Secret {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    OnChainSecret(String address, String value) {
        super(address, value);
    }
}
