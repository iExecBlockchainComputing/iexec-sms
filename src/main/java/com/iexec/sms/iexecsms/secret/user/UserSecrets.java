package com.iexec.sms.iexecsms.secret.user;

import com.iexec.common.utils.HashUtils;
import com.iexec.sms.iexecsms.secret.Secret;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Slf4j
public class UserSecrets {

    @Id
    private String id;
    private String address;
    private List<Secret> secrets;

    public UserSecrets(String address) {
        this.address = address;
        this.secrets = new ArrayList<>();
    }

    public String getHash(){
        return HashUtils.concatenateAndHash(
                HashUtils.sha256(address)//,
                //payload.getHash()
        );
    }

    Secret getSecret(String secretId) {
        for (Secret secret: secrets) {
            if (secret.getAddress().equals(secretId)) {
                return secret;
            }
        }
        return null;
    }
}
