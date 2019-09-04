package com.iexec.sms.iexecsms.tee.challenge;

import com.iexec.sms.iexecsms.utils.EthereumCredentials;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeeChallenge {

    private String taskId;
    private EthereumCredentials credentials;
}
