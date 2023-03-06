package com.iexec.sms.api;

import feign.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class SmsClientProviderTests {
    private static final String SMS_URL_1 = "smsUrl1";
    private static final String SMS_URL_2 = "smsUrl2";
    private SmsClientProvider smsClientProvider;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        smsClientProvider = new SmsClientProvider(Logger.Level.NONE);
    }

    // region getSmsClient
    @Test
    void shouldGetSmsClientForUrl() {
        // Get same SMS client
        SmsClient smsClient1a = smsClientProvider.getSmsClient(SMS_URL_1);
        assertNotNull(smsClient1a);
        // Get same SMS client on same URL
        SmsClient smsClient1b = smsClientProvider.getSmsClient(SMS_URL_1);
        assertEquals(smsClient1a, smsClient1b);
        // Get different SMS clients
        SmsClient smsClient2b = smsClientProvider.getSmsClient(SMS_URL_2);
        assertNotEquals(smsClient1a, smsClient2b);
    }
    // endregion
}