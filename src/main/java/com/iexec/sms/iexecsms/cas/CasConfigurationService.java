package com.iexec.sms.iexecsms.cas;

import org.springframework.stereotype.Service;

@Service
public class CasConfigurationService {

    private CasConfiguration casConfiguration;

    public CasConfigurationService(CasConfiguration casConfiguration) {
        this.casConfiguration = casConfiguration;
    }

    public String getCasUrl() {
        return casConfiguration.getUrl();
    }
}
