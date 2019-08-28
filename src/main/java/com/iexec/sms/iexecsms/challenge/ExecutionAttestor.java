package com.iexec.sms.iexecsms.challenge;

import com.iexec.sms.iexecsms.credential.EthereumCredentials;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionAttestor {

    private String taskId;
    private EthereumCredentials credentials;
}
