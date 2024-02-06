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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Centralisation of all secrets metrics for a type of secret.
 * <p>
 * E.g., initial count, number of secrets added since start and currently stored secrets
 * for `web2Secrets` are metrics that can be retrieved there.
 * <p>
 * Stored secrets count is a scheduled operation, so it can reflect a previous state.
 * That's required to avoid any kind of DDoS.
 */
@Slf4j
public class MeasuredSecretService {
    private static final String INITIAL_SECRETS_COUNT_POSTFIX = "initial";
    private static final String ADDED_SECRETS_SINCE_START_COUNT_POSTFIX = "added";
    private static final String STORED_SECRETS_COUNT_POSTFIX = "stored";
    private static final String CACHED_SECRETS_COUNT_POSTFIX = "cached";

    @Getter
    private final String secretsType;
    private final String metricsPrefix;
    private final Supplier<Long> storedSecretsCountGetter;
    private final ScheduledExecutorService storageMetricsExecutorService;
    private final int storedSecretsCountPeriod;

    private Counter initialSecretsCounter;
    private AtomicLong storedSecretsCount;
    private final Counter addedSecretsSinceStartCounter;
    private final AtomicLong cachedSecretsCount;
    private final LongSupplier cachedSecretsCountGetter;

    public MeasuredSecretService(String secretsType,
                                 String metricsPrefix,
                                 Supplier<Long> storedSecretsCountGetter,
                                 LongSupplier cachedSecretsCountGetter,
                                 ScheduledExecutorService storageMetricsExecutorService,
                                 int storedSecretsCountPeriod) {
        this.secretsType = secretsType;
        this.metricsPrefix = metricsPrefix;
        this.storedSecretsCountGetter = storedSecretsCountGetter;
        this.storageMetricsExecutorService = storageMetricsExecutorService;
        this.storedSecretsCountPeriod = storedSecretsCountPeriod;
        this.cachedSecretsCountGetter = cachedSecretsCountGetter;
        this.addedSecretsSinceStartCounter = Metrics.counter(metricsPrefix + ADDED_SECRETS_SINCE_START_COUNT_POSTFIX);
        this.cachedSecretsCount = Metrics.gauge(metricsPrefix + CACHED_SECRETS_COUNT_POSTFIX, new AtomicLong(0));
    }

    @PostConstruct
    void init() {
        final long initialSecretsCount = storedSecretsCountGetter.get();
        this.initialSecretsCounter = Metrics.counter(metricsPrefix + INITIAL_SECRETS_COUNT_POSTFIX);
        this.initialSecretsCounter.increment(initialSecretsCount);
        this.storedSecretsCount = Metrics.gauge(metricsPrefix + STORED_SECRETS_COUNT_POSTFIX, new AtomicLong(initialSecretsCount));

        storageMetricsExecutorService.scheduleAtFixedRate(
                this::countStoredSecrets,
                storedSecretsCountPeriod,
                storedSecretsCountPeriod,
                TimeUnit.SECONDS
        );
    }

    // region Get metrics
    public long getInitialSecretsCount() {
        return (long) initialSecretsCounter.count();
    }

    public long getAddedSecretsSinceStartCount() {
        return (long) addedSecretsSinceStartCounter.count();
    }

    public long getStoredSecretsCount() {
        return storedSecretsCount.get();
    }

    public long getCachedSecretsCount() {
        return cachedSecretsCount.get();
    }
    // endregion

    /**
     * Indicate to this service a new secret has been added to the DB,
     * so it can update its counter.
     */
    public void newlyAddedSecret() {
        addedSecretsSinceStartCounter.increment();
    }

    private void countStoredSecrets() {
        try {
            final Long count = storedSecretsCountGetter.get();
            final long cacheCount = cachedSecretsCountGetter.getAsLong();
            log.debug("Counting secrets [type:{}, count:{}, cache:{}]", secretsType, count, cacheCount);
            storedSecretsCount.set(count);
            cachedSecretsCount.set(cacheCount);
        } catch (RuntimeException e) {
            log.error("Secrets count has failed [type:{}]", secretsType, e);
            storedSecretsCount.set(-1);
            cachedSecretsCount.set(-1);
        }
    }
}
