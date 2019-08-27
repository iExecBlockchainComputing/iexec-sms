package com.iexec.sms.iexecsms.secret.offchain;

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
public class OffChainSecrets {

    @Id
    private String id;
    private String ownerAddress;
    private List<Secret> secrets;

    public OffChainSecrets(String ownerAddress) {
        this.ownerAddress = ownerAddress;
        this.secrets = new ArrayList<>();
    }

    public String getHash(){
        return HashUtils.concatenateAndHash(
                HashUtils.sha256(ownerAddress)//,
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
