package com.iexec.sms.api;

import feign.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the {@link SmsClient}, providing an easy way to access SMS
 * and avoiding the need to create a new {@link SmsClient} instance each time.
 */
public class SmsClientProvider {
    private final Map<String, SmsClient> urlToSmsClient = new HashMap<>();

    private final Logger.Level loggerLevel;

    public SmsClientProvider() {
        this(Logger.Level.BASIC);
    }

    public SmsClientProvider(Logger.Level loggerLevel) {
        this.loggerLevel = loggerLevel;
    }

    /**
     * Retrieves an SMS client for the specified SMS URL:
     * <ul>
     *     <li>If this SMS has already been accessed, returns the already-constructed {@link SmsClient};</li>
     *     <li>Otherwise, constructs, stores and returns a new {@link SmsClient}.</li>
     * </ul>
     *
     * @param smsUrl URL of the SMS.
     * @return An instance of {@link SmsClient} pointing to the specified SMS.
     */
    public SmsClient getSmsClient(String smsUrl) {
        return urlToSmsClient.computeIfAbsent(smsUrl, url -> SmsClientBuilder.getInstance(loggerLevel, url));
    }

}
