package com.iexec.sms.iexecsms.secret;

import com.iexec.common.utils.HashUtils;
import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Secret {

    private String address; //0xdataset1 or aws.amazon.com
    private String value;

    public String getHash() {
        return HashUtils.concatenateAndHash(
                HashUtils.sha256(address),
                HashUtils.sha256(value)
        );
    }

}
