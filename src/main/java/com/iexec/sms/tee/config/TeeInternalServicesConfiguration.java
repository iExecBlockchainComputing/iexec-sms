package com.iexec.sms.tee.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.config.TeeServicesProperties;

public interface TeeInternalServicesConfiguration {
    TeeEnclaveProvider getTeeEnclaveProvider();
    TeeServicesProperties getProperties();
}
