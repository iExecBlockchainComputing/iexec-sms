/*
 * Copyright 2023-2024 IEXEC BLOCKCHAIN TECH
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
class MeasuredSecretServiceTests {
    private static final String SECRETS_TYPE = "tests_secrets";
    private static final String METRICS_PREFIX = "iexec.sms.secrets.tests_secrets.";
    private static final long INITIAL_COUNT = 5L;

    private final AtomicLong count = new AtomicLong(INITIAL_COUNT);
    private boolean shouldThrowDatabaseAccessException;

    private MeasuredSecretService measuredSecretService;
    private CacheSecretService<String> cacheSecretService;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void beforeEach() {
        this.shouldThrowDatabaseAccessException = false;
        cacheSecretService = new CacheSecretService<>();

        meterRegistry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(meterRegistry);

        this.measuredSecretService = new MeasuredSecretService(
                SECRETS_TYPE,
                METRICS_PREFIX,
                () -> {
                    if (!shouldThrowDatabaseAccessException) {
                        return count.get();
                    }
                    throw new RuntimeException("Mocked data access exception");
                }, // Simulating a repo `count` method
                cacheSecretService::count,
                Executors.newSingleThreadScheduledExecutor(),
                1);
        measuredSecretService.init();
    }

    @AfterEach
    void afterEach() {
        meterRegistry.clear();
        Metrics.globalRegistry.clear();
    }

    @Test
    void shouldGetInitialSecretsCount() {
        assertThat(measuredSecretService.getInitialSecretsCount()).isEqualTo(INITIAL_COUNT);
    }

    @Test
    void shouldGetCachedSecretsCount() {
        final int cachedSecretsCount = 3;
        for (int i = 0; i < cachedSecretsCount; i++) {
            cacheSecretService.putSecretExistenceInCache("secret-key-" + i, true);
        }
        await()
                .timeout(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(measuredSecretService.getCachedSecretsCount()).isEqualTo(cachedSecretsCount));
    }

    @Test
    void shouldGetZeroWhenCacheSecretServiceIsNull() {
        MeasuredSecretService measuredSecretServiceWithNullCache = new MeasuredSecretService(
                SECRETS_TYPE,
                METRICS_PREFIX,
                count::get,
                () -> 0L,
                Executors.newSingleThreadScheduledExecutor(),
                1);
        measuredSecretServiceWithNullCache.init();

        await().atLeast(5, TimeUnit.SECONDS);
        assertThat(measuredSecretService.getCachedSecretsCount()).isZero();
    }

    @Test
    void shouldAddNewSecretsAndGetAddedSecretsSinceStartCount() {
        assertThat(measuredSecretService.getAddedSecretsSinceStartCount()).isZero();
        measuredSecretService.newlyAddedSecret();
        assertThat(measuredSecretService.getAddedSecretsSinceStartCount()).isOne();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 10, Long.MAX_VALUE})
    void shouldGetStoredSecretsCount(long storedCount) {
        count.set(storedCount);

        // The value requires at least 1 second to be updated by the scheduled executor
        await()
                .timeout(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(measuredSecretService.getStoredSecretsCount()).isEqualTo(storedCount));
    }

    @Test
    void shouldNotGetStoredSecretsCount() {
        this.shouldThrowDatabaseAccessException = true;

        // The value requires at least 1 second to be updated by the scheduled executor
        await()
                .timeout(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(measuredSecretService.getStoredSecretsCount()).isEqualTo(-1));
    }
}
