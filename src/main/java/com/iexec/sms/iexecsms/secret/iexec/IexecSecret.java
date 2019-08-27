package com.iexec.sms.iexecsms.secret.iexec;

import com.iexec.sms.iexecsms.secret.Secret;
import lombok.*;
import org.springframework.data.annotation.Id;

@EqualsAndHashCode(callSuper = true)
@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IexecSecret extends Secret {

    @Id
    private String id;

    public IexecSecret(String address, String value) {
        super(address, value);
    }
}
