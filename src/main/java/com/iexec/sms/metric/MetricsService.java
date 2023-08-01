/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

import com.iexec.sms.secret.compute.TeeTaskComputeSecretService;
import com.iexec.sms.secret.web2.Web2SecretService;
import com.iexec.sms.secret.web3.Web3SecretService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final CompositeMeterRegistry metricsRegistry;

    public MetricsService() {
        this.metricsRegistry = Metrics.globalRegistry;
    }

    public SmsMetric getSmsMetric() {
        return SmsMetric.builder()
                .web2SecretsMetric(getWeb2SecretsMetrics())
                .web3SecretsMetric(getWeb3SecretsMetrics())
                .computeSecretsMetric(getComputeSecretsMetrics())
                .build();
    }

    private SecretsMetric getWeb2SecretsMetrics() {
        return SecretsMetric.builder()
                .secretsType("web2")
                .initialCount((long)metricsRegistry.find(Web2SecretService.INITIAL_SECRETS_COUNT_KEY).counter().count())
                .addedSinceStartCount((long)metricsRegistry.find(Web2SecretService.ADDED_SECRETS_SINCE_START_COUNT_KEY).counter().count())
                .storedCount((long)metricsRegistry.find(Web2SecretService.STORED_SECRETS_COUNT_KEY).gauge().value())
                .build();
    }

    private SecretsMetric getWeb3SecretsMetrics() {
        return SecretsMetric.builder()
                .secretsType("web3")
                .initialCount((long)metricsRegistry.find(Web3SecretService.INITIAL_SECRETS_COUNT_KEY).counter().count())
                .addedSinceStartCount((long)metricsRegistry.find(Web3SecretService.ADDED_SECRETS_SINCE_START_COUNT_KEY).counter().count())
                .storedCount((long)metricsRegistry.find(Web3SecretService.STORED_SECRETS_COUNT_KEY).gauge().value())
                .build();
    }

    private SecretsMetric getComputeSecretsMetrics() {
        return SecretsMetric.builder()
                .secretsType("compute")
                .initialCount((long)metricsRegistry.find(TeeTaskComputeSecretService.INITIAL_SECRETS_COUNT_KEY).counter().count())
                .addedSinceStartCount((long)metricsRegistry.find(TeeTaskComputeSecretService.ADDED_SECRETS_SINCE_START_COUNT_KEY).counter().count())
                .storedCount((long)metricsRegistry.find(TeeTaskComputeSecretService.STORED_SECRETS_COUNT_KEY).gauge().value())
                .build();
    }

}
