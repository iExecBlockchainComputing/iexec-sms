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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;

class MetricsServiceTests {
    private static final String SECRETS_TYPE = "testSecrets";
    private static final String METRICS_PREFIX = "iexec.sms.secrets.tests_secrets.";
    private static final long INITIAL_COUNT = 5L;

    private final AtomicLong count = new AtomicLong(INITIAL_COUNT);
    private final MeasuredSecretService measuredSecretService = Mockito.spy(new MeasuredSecretService(
            SECRETS_TYPE,
            METRICS_PREFIX,
            count::get, // Simulating a repo `count` method
            Executors.newSingleThreadScheduledExecutor(),
            1,
            null
    ));

    // region registerNewMeasuredSecretService
    @Test
    void shouldRegisterNewMeasuredSecretService() {
        final MetricsService metricsService = new MetricsService();

        final List<?> measuredSecretServices =
                (List<?>) ReflectionTestUtils.getField(metricsService, "measuredSecretServices");

        assertThat(measuredSecretServices).isEmpty();

        metricsService.registerNewMeasuredSecretService(measuredSecretService);

        assertThat(measuredSecretServices).hasSize(1);
    }
    // endregion

    // region getSmsMetrics
    @Test
    void shouldGetMetrics() {
        final MetricsService metricsService = new MetricsService(List.of(measuredSecretService));

        final long initialCount = 1;
        final long addedCount = 2;
        final long storedCount = 3;
        final long cachedCount = 2;

        doReturn(initialCount).when(measuredSecretService).getInitialSecretsCount();
        doReturn(addedCount).when(measuredSecretService).getAddedSecretsSinceStartCount();
        doReturn(storedCount).when(measuredSecretService).getStoredSecretsCount();
        doReturn(cachedCount).when(measuredSecretService).getCachedSecretsCount();

        final SmsMetrics smsMetrics = metricsService.getSmsMetrics();
        final Map<String, SecretsMetrics> secretsMetrics = smsMetrics.getSecretsMetrics();

        assertAll(
                () -> assertThat(secretsMetrics).hasSize(1),
                () -> assertThat(secretsMetrics.get(SECRETS_TYPE)).extracting(SecretsMetrics::getSecretsType).isEqualTo(SECRETS_TYPE),
                () -> assertThat(secretsMetrics.get(SECRETS_TYPE)).extracting(SecretsMetrics::getInitialCount).isEqualTo(initialCount),
                () -> assertThat(secretsMetrics.get(SECRETS_TYPE)).extracting(SecretsMetrics::getAddedSinceStartCount).isEqualTo(addedCount),
                () -> assertThat(secretsMetrics.get(SECRETS_TYPE)).extracting(SecretsMetrics::getStoredCount).isEqualTo(storedCount),
                () -> assertThat(secretsMetrics.get(SECRETS_TYPE)).extracting(SecretsMetrics::getCachedSecretsCount).isEqualTo(cachedCount)

        );
    }
    // endregion
}
