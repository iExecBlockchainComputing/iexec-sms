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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MetricsService {

    private final List<MeasuredSecretService> measuredSecretServices;

    @Autowired
    public MetricsService() {
        this.measuredSecretServices = new ArrayList<>();
    }

    public MetricsService(List<MeasuredSecretService> measuredSecretServices) {
        this.measuredSecretServices = new ArrayList<>(measuredSecretServices);
    }

    public MeasuredSecretService registerNewMeasuredSecretService(MeasuredSecretService service) {
        measuredSecretServices.add(service);
        return service;
    }

    public SmsMetrics getSmsMetrics() {
        final Map<String, SecretsMetrics> secretsMetrics = measuredSecretServices
                .stream()
                .map(this::getSecretsMetrics)
                .collect(Collectors.toMap(
                        SecretsMetrics::getSecretsType,
                        Function.identity()
                ));

        return SmsMetrics.builder()
                .secretsMetrics(secretsMetrics)
                .build();
    }

    private SecretsMetrics getSecretsMetrics(MeasuredSecretService secretService) {
        return SecretsMetrics.builder()
                .secretsType(secretService.getSecretsType())
                .initialCount(secretService.getInitialSecretsCount())
                .addedSinceStartCount(secretService.getAddedSecretsSinceStartCount())
                .storedCount(secretService.getStoredSecretsCount())
                .cachedSecretsCount(secretService.getCachedSecretsCount())
                .build();
    }

}
