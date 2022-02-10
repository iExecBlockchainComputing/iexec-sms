package com.iexec.sms.api;

import feign.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmsClientTest {

    @Test
    void instantiationTest() {
        Assertions.assertNotNull(SmsClientBuilder.getInstance(Logger.Level.FULL, "localhost"));
    }

}
