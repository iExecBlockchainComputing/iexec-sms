package com.iexec.sms.tee;

import com.iexec.common.tee.TeeFramework;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * This class is needed to have a case-insensitive `teeFramework` path variable in
 * {@link TeeController#getTeeServicesProperties(TeeFramework)}.
 */
@Component
public class TeeFrameworkConverter implements Converter<String, TeeFramework> {
    @Override
    public TeeFramework convert(String value) {
        return TeeFramework.valueOf(value.toUpperCase());
    }
}
