package com.iexec.sms.iexecsms.cas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CasYmlConfiguration {

    @Value("${scone.cas.host}")
    private String host;

    @Value("${scone.cas.port}")
    private String port;

    public String getUrl() {
        return "https://" + host + ":" + port;
    }
}
