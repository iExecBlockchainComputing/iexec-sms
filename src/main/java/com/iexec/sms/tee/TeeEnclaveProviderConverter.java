package com.iexec.sms.tee;

import com.iexec.common.tee.TeeEnclaveProvider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * This class is needed to have a case-insensitive `teeEnclaveProvider` path variable in
 * {@link TeeController#getTeeServicesConfig(TeeEnclaveProvider)}.
 */
@Component
public class TeeEnclaveProviderConverter implements Converter<String, TeeEnclaveProvider> {
    @Override
    public TeeEnclaveProvider convert(String value) {
        return TeeEnclaveProvider.valueOf(value.toUpperCase());
    }
}
