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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import org.springframework.data.repository.CrudRepository;

@Getter
public class MeasuredSecretService {
    private static final String INITIAL_SECRETS_COUNT_POSTFIX = "initial";
    private static final String ADDED_SECRETS_SINCE_START_COUNT_POSTFIX = "added";
    private static final String STORED_SECRETS_COUNT_POSTFIX = "stored";

    private final String secretsType;
    private final String metricsPrefix;
    private final CrudRepository<?, ?> repository;

    private final Counter initialSecretsCounter;
    private final Counter addedSecretsSinceStartCounter;
    private final Gauge storedSecretsGauge;

    public MeasuredSecretService(String secretsType,
                                 String metricsPrefix,
                                 CrudRepository<?, ?> repository) {
        this.secretsType = secretsType;
        this.metricsPrefix = metricsPrefix;
        this.repository = repository;

        final long initialSecretsCount = repository.count();
        this.initialSecretsCounter = Metrics.counter(getInitialSecretsCountKey());
        this.initialSecretsCounter.increment(initialSecretsCount);

        this.addedSecretsSinceStartCounter = Metrics.counter(getAddedSecretsSinceStartCountKey());

        Metrics.gauge(getStoredSecretsCountKey(), repository, CrudRepository::count);
        this.storedSecretsGauge = Metrics.globalRegistry.find(getStoredSecretsCountKey()).gauge();
    }

    // region Get metrics keys
    private String getInitialSecretsCountKey() {
        return metricsPrefix + INITIAL_SECRETS_COUNT_POSTFIX;
    }

    private String getAddedSecretsSinceStartCountKey() {
        return metricsPrefix + ADDED_SECRETS_SINCE_START_COUNT_POSTFIX;
    }

    private String getStoredSecretsCountKey() {
        return metricsPrefix + STORED_SECRETS_COUNT_POSTFIX;
    }
    // endregion

    // region Get metrics
    public long getInitialSecretsCount() {
        return (long) initialSecretsCounter.count();
    }

    public long getAddedSecretsSinceStartCount() {
        return (long) addedSecretsSinceStartCounter.count();
    }

    public long getStoredSecretsCount() {
        return (long) storedSecretsGauge.value();
    }
    // endregion

    public void newlyAddedSecret() {
        addedSecretsSinceStartCounter.increment();
    }
}
