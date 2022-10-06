/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.sms.api;

import com.iexec.common.tee.TeeFramework;
import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class SmsClientTests {
    private static final String LAS_IMAGE = "lasImage";

    @Mock
    TeeAppProperties preComputeProperties;
    @Mock
    TeeAppProperties postComputeProperties;

    // region getTeeServicesProperties
    @Test
    void shouldGetSconeServicesProperties() {
        final SmsClient smsClient = spy(SmsClient.class);
        final SconeServicesProperties properties = new SconeServicesProperties(
                preComputeProperties,
                postComputeProperties,
                LAS_IMAGE
        );

        when(smsClient.getSconeServicesProperties()).thenReturn(properties);

        final TeeServicesProperties teeServicesProperties =
                smsClient.getTeeServicesProperties(TeeFramework.SCONE);

        assertEquals(properties, teeServicesProperties);
    }

    @Test
    void shouldGetGramineServicesProperties() {
        final SmsClient smsClient = spy(SmsClient.class);
        final GramineServicesProperties properties = new GramineServicesProperties(
                preComputeProperties,
                postComputeProperties
        );

        when(smsClient.getGramineServicesProperties()).thenReturn(properties);

        final TeeServicesProperties teeServicesProperties =
                smsClient.getTeeServicesProperties(TeeFramework.GRAMINE);

        assertEquals(properties, teeServicesProperties);
    }

    @Test
    void shouldNotGetTeeServicesPropertiesSinceUnknownProvider() {
        final SmsClient smsClient = spy(SmsClient.class);

        when(smsClient.getGramineServicesProperties()).thenReturn(null);

        final TeeServicesProperties teeServicesProperties =
                smsClient.getTeeServicesProperties(TeeFramework.GRAMINE);

        assertNull(teeServicesProperties);
    }
    // endregion
}
