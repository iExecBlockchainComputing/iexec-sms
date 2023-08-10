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

package com.iexec.sms.metric;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

class MetricsControllerTests {
    private static final String SECRETS_TYPE = "testSecrets";

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private MetricsController metricsController;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    // region getSmsMetrics
    @Test
    void shouldGetSmsMetrics() {
        final long initialCount = 1;
        final long addedCount = 2;
        final long storedCount = 3;

        final SecretsMetrics secretsMetrics = SecretsMetrics
                .builder()
                .secretsType(SECRETS_TYPE)
                .initialCount(initialCount)
                .addedSinceStartCount(addedCount)
                .storedCount(storedCount)
                .build();
        final SmsMetrics metrics = SmsMetrics
                .builder()
                .secretsMetrics(List.of(secretsMetrics))
                .build();
        when(metricsService.getSmsMetrics()).thenReturn(metrics);

        final ResponseEntity<SmsMetrics> response = metricsController.getSmsMetrics();

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isEqualTo(metrics)
        );
    }
    // endregion
}