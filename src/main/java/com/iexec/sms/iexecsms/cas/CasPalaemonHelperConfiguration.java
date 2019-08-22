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
public class CasPalaemonHelperConfiguration {

    @Value("${palaemon.configFile.withDataset}")
    private String palaemonConfigFileWithDataset;

    @Value("${palaemon.configFile.withoutDataset}")
    private String palaemonConfigFileWithoutDataset;

}
