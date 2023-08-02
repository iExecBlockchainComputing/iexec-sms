/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret;

import com.iexec.sms.metric.MetricsService;
import com.iexec.sms.metric.SecretsMetrics;
import com.iexec.sms.metric.SmsMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;

class MetricsServiceTests {
    private static final String TEST_SECRETS_TYPE = "testSecrets";

    private final MeasuredSecretService measuredSecretService = Mockito.spy(new MeasuredSecretService(TEST_SECRETS_TYPE) {
        @Override
        public String getMetricsPrefix() {
            return "testSecretsPrefix";
        }
    });

    private final MetricsService metricsService = new MetricsService(List.of(measuredSecretService));

    @Test
    void shouldGetMetrics() {
        final long initialCount = 1;
        final long addedCount = 2;
        final long storedCount = 3;

        doReturn(initialCount).when(measuredSecretService).getInitialSecretsCount();
        doReturn(addedCount  ).when(measuredSecretService).getAddedSecretsSinceStartCount();
        doReturn(storedCount ).when(measuredSecretService).getStoredSecretsCount();

        final SmsMetrics smsMetrics = metricsService.getSmsMetrics();
        final List<SecretsMetrics> secretsMetrics = smsMetrics.getSecretsMetrics();

        assertAll(
                () -> assertThat(secretsMetrics).hasSize(1),
                () -> assertThat(secretsMetrics.get(0)).extracting(SecretsMetrics::getSecretsType         ).isEqualTo(TEST_SECRETS_TYPE),
                () -> assertThat(secretsMetrics.get(0)).extracting(SecretsMetrics::getInitialCount        ).isEqualTo(initialCount),
                () -> assertThat(secretsMetrics.get(0)).extracting(SecretsMetrics::getAddedSinceStartCount).isEqualTo(addedCount),
                () -> assertThat(secretsMetrics.get(0)).extracting(SecretsMetrics::getStoredCount         ).isEqualTo(storedCount)
        );
    }
}
