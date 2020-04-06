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
public class TeeSessionHelperConfiguration {

    @Value("${scone.cas.palaemon.configFile.withDataset}")
    private String palaemonTemplateWithAppAndDataset;

    @Value("${scone.cas.palaemon.configFile.withoutDataset}")
    private String palaemonTemplateWithApp;

}
