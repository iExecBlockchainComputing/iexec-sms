package com.iexec.sms.iexecsms.secret.offchain;

import com.iexec.sms.iexecsms.secret.Secret;
import lombok.Data;
import lombok.Getter;
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
