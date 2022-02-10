package com.iexec.sms.api;

import com.iexec.common.utils.FeignBuilder;
import feign.Logger;

/**
 * Creates a Feign client instance for {@link SmsClient}.
 */
public class SmsClientBuilder {

    private static SmsClient smsClient;

    private SmsClientBuilder() {}

    public static SmsClient getInstance(Logger.Level logLevel, String url) {
        return FeignBuilder.createBuilder(logLevel)
                .target(SmsClient.class, url);
    }

}
