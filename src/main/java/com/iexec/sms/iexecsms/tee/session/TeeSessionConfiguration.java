package com.iexec.sms.iexecsms.tee.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeeSessionConfiguration {

    @Value("${scone.cas.host}")
    private String host;

    @Value("${scone.cas.port}")
    private String port;

    @Value("${scone.cas.palaemon.configFile.withDataset}")
    private String palaemonConfigFileWithDataset;

    @Value("${scone.cas.palaemon.configFile.withoutDataset}")
    private String palaemonConfigFileWithoutDataset;

    public String getCasUrl() {
        return "https://" + host + ":" + port;
    }
}
