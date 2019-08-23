package com.iexec.sms.iexecsms.secret;

import com.iexec.common.utils.HashUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Slf4j
public class SecretFolder {

    @Id
    private String id;
    private String address;
    private List<Secret> secrets;

    public SecretFolder(String address) {
        this.address = address;
        this.secrets = new ArrayList<>();
    }

    public String getHash(){
        return HashUtils.concatenateAndHash(
                HashUtils.sha256(address)//,
                //payload.getHash()
        );
    }

    Secret getSecret(String secretAlias) {
        for (Secret secret: secrets) {
            if (secret.getAlias().equals(secretAlias)) {
                return secret;
            }
        }
        return null;
    }
}
