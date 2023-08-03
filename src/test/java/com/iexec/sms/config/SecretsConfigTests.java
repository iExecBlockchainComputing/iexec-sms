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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class SecretsConfigTests {
    private final ScheduledExecutorService storageMetricsExecutorService = Executors.newSingleThreadScheduledExecutor();

    private SecretsConfig secretsConfig;

    @BeforeEach
    void init() {
        this.secretsConfig = new SecretsConfig(storageMetricsExecutorService);
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
        storageMetricsExecutorService.scheduleAtFixedRate(
                () -> Awaitility.await()
                        .timeout(10, TimeUnit.SECONDS)
                        .until(storageMetricsExecutorService::isTerminated),    // Should not stop before the executor
                0,
                10,
                TimeUnit.MILLISECONDS
        );

        secretsConfig.shutdown();

        assertAll(
                () -> assertThat(storageMetricsExecutorService.isShutdown()).isTrue(),
                () -> assertThat(storageMetricsExecutorService.isTerminated()).isTrue()
        );
    }
    // endregion

    // region storageMetricsExecutorService
    @Test
    void shouldGetStorageMetricsExecutorService() {
        assertThat(secretsConfig.storageMetricsExecutorService()).isEqualTo(storageMetricsExecutorService);
    }
    // endregion
}
