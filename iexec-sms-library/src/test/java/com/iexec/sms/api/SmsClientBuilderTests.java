package com.iexec.sms.api;

import feign.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsClientBuilderTests {

    // region getInstance
    @Test
    void instantiationTest() {
        assertNotNull(SmsClientBuilder.getInstance(Logger.Level.FULL, "localhost"));
    }
    // endregion
}