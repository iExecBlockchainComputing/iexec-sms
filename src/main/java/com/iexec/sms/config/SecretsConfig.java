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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
public class SecretsConfig {
    private final MetricsService metricsService;
    private final ScheduledExecutorService storageMetricsExecutorService;

    @Autowired
    public SecretsConfig(MetricsService metricsService) {
        this.metricsService = metricsService;
        this.storageMetricsExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    SecretsConfig(MetricsService metricsService,
                  ScheduledExecutorService storageMetricsExecutorService) {
        this.metricsService = metricsService;
        this.storageMetricsExecutorService = storageMetricsExecutorService;
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        storageMetricsExecutorService.shutdown();
        try {
            if (!storageMetricsExecutorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                storageMetricsExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            storageMetricsExecutorService.shutdownNow();
            throw e;
        }
    }

    @Bean
    MeasuredSecretService web2MeasuredSecretService(Web2SecretRepository web2SecretRepository,
                                                    @Value("${metrics.storage.refresh-interval}") int storedSecretsCountPeriod) {
        return metricsService.registerNewMeasuredSecretService(
                new MeasuredSecretService(
                        "web2",
                        "iexec.sms.secrets.web2.",
                        web2SecretRepository::count,
                        storageMetricsExecutorService,
                        storedSecretsCountPeriod
                )
        );
    }

    @Bean
    MeasuredSecretService web3MeasuredSecretService(Web3SecretRepository web3SecretRepository,
                                                    @Value("${metrics.storage.refresh-interval}") int storedSecretsCountPeriod) {
        return metricsService.registerNewMeasuredSecretService(
                new MeasuredSecretService(
                        "web3",
                        "iexec.sms.secrets.web3.",
                        web3SecretRepository::count,
                        storageMetricsExecutorService,
                        storedSecretsCountPeriod
                )
        );
    }

    @Bean
    MeasuredSecretService computeMeasuredSecretService(TeeTaskComputeSecretRepository teeTaskComputeSecretRepository,
                                                       @Value("${metrics.storage.refresh-interval}") int storedSecretsCountPeriod) {
        return metricsService.registerNewMeasuredSecretService(
                new MeasuredSecretService(
                        "compute",
                        "iexec.sms.secrets.compute.",
                        teeTaskComputeSecretRepository::count,
                        storageMetricsExecutorService,
                        storedSecretsCountPeriod
                )
        );
    }

    @Bean
    MeasuredSecretService teeChallengeMeasuredSecretService(TeeChallengeRepository teeChallengeRepository,
                                                            @Value("${metrics.storage.refresh-interval}") int storedSecretsCountPeriod) {
        return metricsService.registerNewMeasuredSecretService(
                new MeasuredSecretService(
                        "TEE challenges & Ethereum Credentials",
                        "iexec.sms.secrets.tee_challenge_ethereum_credentials.",
                        teeChallengeRepository::count,
                        storageMetricsExecutorService,
                        storedSecretsCountPeriod
                )
        );
    }
}
