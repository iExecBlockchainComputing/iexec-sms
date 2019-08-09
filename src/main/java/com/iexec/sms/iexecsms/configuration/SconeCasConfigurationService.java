package com.iexec.sms.iexecsms.configuration;

import org.springframework.stereotype.Service;

@Service
public class SconeCasConfigurationService {

    private SconeCasConfiguration sconeCasConfiguration;

    public SconeCasConfigurationService(SconeCasConfiguration sconeCasConfiguration) {
        this.sconeCasConfiguration = sconeCasConfiguration;
    }

    public String getCasURL() {
        return sconeCasConfiguration.getURL();
    }
}
