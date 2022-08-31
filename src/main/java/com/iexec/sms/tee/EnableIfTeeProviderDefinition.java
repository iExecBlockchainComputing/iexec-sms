package com.iexec.sms.tee;

import com.iexec.common.tee.TeeEnclaveProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnableIfTeeProviderDefinition {
    TeeEnclaveProvider[] providers();
}
