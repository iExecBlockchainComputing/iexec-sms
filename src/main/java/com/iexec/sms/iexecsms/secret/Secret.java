package com.iexec.sms.iexecsms.secret;

import com.iexec.common.utils.HashUtils;
import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Secret {

    private String alias;
    private String value;

    //private String symmetricKey; //(Kd, Kb, Ke)
    //private String beneficiaryCredentials; //dropbox, AWS, TODO think about

    public String getHash() {
        return HashUtils.concatenateAndHash(
                HashUtils.sha256(alias),
                HashUtils.sha256(value)
        );
    }

}
