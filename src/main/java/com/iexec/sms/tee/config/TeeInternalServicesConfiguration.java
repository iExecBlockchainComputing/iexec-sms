package com.iexec.sms.tee.config;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.api.config.TeeServicesConfiguration;

public interface TeeInternalServicesConfiguration {
    TeeEnclaveProvider getTeeEnclaveProvider();
    TeeServicesConfiguration getShareableConfiguration();
}
