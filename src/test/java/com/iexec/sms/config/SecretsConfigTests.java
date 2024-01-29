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

package com.iexec.sms.config;

import com.iexec.sms.metric.MetricsService;
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.secret.compute.TeeTaskComputeSecretRepository;
import com.iexec.sms.secret.web2.Web2SecretRepository;
import com.iexec.sms.secret.web3.Web3SecretRepository;
import com.iexec.sms.tee.challenge.TeeChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecretsConfigTests {
    private static final int STORED_SECRETS_COUNT_PERIOD = 30;
    private final ScheduledExecutorService storageMetricsExecutorService = Executors.newSingleThreadScheduledExecutor();

    @Mock
    private MetricsService metricsService;

    private SecretsConfig secretsConfig;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        this.secretsConfig = new SecretsConfig(metricsService, storageMetricsExecutorService);

        when(metricsService.registerNewMeasuredSecretService(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // region shutdown
    @Test
    void shouldShutdownGracefullyWhenNoTask() throws InterruptedException {
        secretsConfig.shutdown();

        assertAll(
                () -> assertThat(storageMetricsExecutorService.isShutdown()).isTrue(),
                () -> assertThat(storageMetricsExecutorService.isTerminated()).isTrue()
        );
    }

    @Test
    void shouldShutdownWhenScheduledTasks() throws InterruptedException {
        storageMetricsExecutorService.scheduleAtFixedRate(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        secretsConfig.shutdown();

        assertThat(storageMetricsExecutorService.isShutdown()).isTrue();
    }
    // endregion

    // region MeasuredSecretService bean definitions
    @Test
    void web2MeasuredSecretService() {
        final Web2SecretRepository repository = mock(Web2SecretRepository.class);
        final MeasuredSecretService measuredSecretService = secretsConfig.web2MeasuredSecretService(repository, STORED_SECRETS_COUNT_PERIOD);

        final String secretsType = ((String) ReflectionTestUtils.getField(measuredSecretService, "secretsType"));
        final String metricsPrefix = ((String) ReflectionTestUtils.getField(measuredSecretService, "metricsPrefix"));

        assertAll(
                () -> assertThat(secretsType).isEqualTo("web2"),
                () -> assertThat(metricsPrefix).isEqualTo("iexec.sms.secrets.web2."),
                () -> verify(metricsService).registerNewMeasuredSecretService(measuredSecretService)
        );
    }

    @Test
    void web3MeasuredSecretService() {
        final Web3SecretRepository repository = mock(Web3SecretRepository.class);
        final MeasuredSecretService measuredSecretService = secretsConfig.web3MeasuredSecretService(repository, STORED_SECRETS_COUNT_PERIOD);

        final String secretsType = ((String) ReflectionTestUtils.getField(measuredSecretService, "secretsType"));
        final String metricsPrefix = ((String) ReflectionTestUtils.getField(measuredSecretService, "metricsPrefix"));

        assertAll(
                () -> assertThat(secretsType).isEqualTo("web3"),
                () -> assertThat(metricsPrefix).isEqualTo("iexec.sms.secrets.web3."),
                () -> verify(metricsService).registerNewMeasuredSecretService(measuredSecretService)
        );
    }

    @Test
    void computeMeasuredSecretService() {
        final TeeTaskComputeSecretRepository repository = mock(TeeTaskComputeSecretRepository.class);
        final MeasuredSecretService measuredSecretService = secretsConfig.computeMeasuredSecretService(repository, STORED_SECRETS_COUNT_PERIOD);

        final String secretsType = ((String) ReflectionTestUtils.getField(measuredSecretService, "secretsType"));
        final String metricsPrefix = ((String) ReflectionTestUtils.getField(measuredSecretService, "metricsPrefix"));

        assertAll(
                () -> assertThat(secretsType).isEqualTo("compute"),
                () -> assertThat(metricsPrefix).isEqualTo("iexec.sms.secrets.compute."),
                () -> verify(metricsService).registerNewMeasuredSecretService(measuredSecretService)
        );
    }

    @Test
    void teeChallengeMeasuredSecretService() {
        final TeeChallengeRepository repository = mock(TeeChallengeRepository.class);
        final MeasuredSecretService measuredSecretService = secretsConfig.teeChallengeMeasuredSecretService(repository, STORED_SECRETS_COUNT_PERIOD);

        final String secretsType = ((String) ReflectionTestUtils.getField(measuredSecretService, "secretsType"));
        final String metricsPrefix = ((String) ReflectionTestUtils.getField(measuredSecretService, "metricsPrefix"));

        assertAll(
                () -> assertThat(secretsType).isEqualTo("TEE challenges & Ethereum Credentials"),
                () -> assertThat(metricsPrefix).isEqualTo("iexec.sms.secrets.tee_challenge_ethereum_credentials."),
                () -> verify(metricsService).registerNewMeasuredSecretService(measuredSecretService)
        );
    }
    // endregion
}
