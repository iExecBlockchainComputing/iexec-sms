package com.iexec.sms.iexecsms.secret.onchain;

import com.iexec.sms.iexecsms.secret.Secret;
import lombok.*;
import org.springframework.data.annotation.Id;

@EqualsAndHashCode(callSuper = true)
@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OnChainSecret extends Secret {

    @Id
    private String id;

    public OnChainSecret(String address, String value) {
        super(address, value);
    }
}
