package com.iexec.sms.secret.web2;

import com.iexec.sms.secret.Secret;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Entity
@NoArgsConstructor
public class Web2Secrets {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    private String ownerAddress;
    @OneToMany(cascade = {CascadeType.ALL})
    private List<Secret> secrets;

    Web2Secrets(String ownerAddress) {
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
