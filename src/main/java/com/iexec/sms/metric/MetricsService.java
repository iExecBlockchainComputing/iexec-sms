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

import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.secret.compute.TeeTaskComputeSecretService;
import com.iexec.sms.secret.web2.Web2SecretService;
import com.iexec.sms.secret.web3.Web3SecretService;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Web2SecretService web2SecretService;
    private final Web3SecretService web3SecretService;
    private final TeeTaskComputeSecretService teeTaskComputeSecretService;

    public MetricsService(Web2SecretService web2SecretService,
                          Web3SecretService web3SecretService,
                          TeeTaskComputeSecretService teeTaskComputeSecretService) {
        this.web2SecretService = web2SecretService;
        this.web3SecretService = web3SecretService;
        this.teeTaskComputeSecretService = teeTaskComputeSecretService;
    }

    public SmsMetrics getSmsMetric() {
        return SmsMetrics.builder()
                .web2SecretsMetrics(getSecretsMetrics("web2", web2SecretService))
                .web3SecretsMetrics(getSecretsMetrics("web3", web3SecretService))
                .computeSecretsMetrics(getSecretsMetrics("compute", teeTaskComputeSecretService))
                .build();
    }

    private SecretsMetrics getSecretsMetrics(String secretsType, MeasuredSecretService secretService) {
        return SecretsMetrics.builder()
                .secretsType(secretsType)
                .initialCount(secretService.getInitialSecretsCount())
                .addedSinceStartCount(secretService.getAddedSecretsSinceStartCount())
                .storedCount(secretService.getStoredSecretsCount())
                .build();
    }

}
