package com.iexec.sms.iexecsms.secret.offchain;

import com.iexec.sms.iexecsms.secret.Secret;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@Getter
@Entity
@NoArgsConstructor
public class OffChainSecrets {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String ownerAddress;
    @OneToMany(cascade = {CascadeType.ALL})
    private List<Secret> secrets;

    OffChainSecrets(String ownerAddress) {
        this.ownerAddress = ownerAddress;
        this.secrets = new ArrayList<>();
    }

    public Secret getSecret(String secretAddress) {
        for (Secret secret : secrets) {
            if (secret.getAddress().equals(secretAddress)) {
                return secret;
            }
        }
        return null;
    }
}
