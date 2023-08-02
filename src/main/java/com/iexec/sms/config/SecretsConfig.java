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

import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.secret.compute.TeeTaskComputeSecretRepository;
import com.iexec.sms.secret.web2.Web2SecretRepository;
import com.iexec.sms.secret.web3.Web3SecretRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class SecretsConfig {
    @Bean
    MeasuredSecretService web2MeasuredSecretService(Web2SecretRepository web2SecretRepository,
                                                    ScheduledExecutorService scheduledExecutorService,
                                                    @Value("secrets.stored.count-period") long storedSecretsCountPeriod) {
        return new MeasuredSecretService(
                "web2",
                "iexec.sms.secrets.web2.",
                web2SecretRepository::count,
                scheduledExecutorService,
                storedSecretsCountPeriod);
    }

    @Bean
    MeasuredSecretService web3MeasuredSecretService(Web3SecretRepository web3SecretRepository,
                                                    ScheduledExecutorService scheduledExecutorService,
                                                    @Value("secrets.stored.count-period") long storedSecretsCountPeriod) {
        return new MeasuredSecretService(
                "web3",
                "iexec.sms.secrets.web3.",
                web3SecretRepository::count,
                scheduledExecutorService,
                storedSecretsCountPeriod);
    }

    @Bean
    MeasuredSecretService computeMeasuredSecretService(TeeTaskComputeSecretRepository teeTaskComputeSecretRepository,
                                                       ScheduledExecutorService scheduledExecutorService,
                                                       @Value("secrets.stored.count-period") long storedSecretsCountPeriod) {
        return new MeasuredSecretService(
                "compute",
                "iexec.sms.secrets.compute.",
                teeTaskComputeSecretRepository::count,
                scheduledExecutorService, storedSecretsCountPeriod);
    }

    @Bean
    ScheduledExecutorService secretsCountExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}