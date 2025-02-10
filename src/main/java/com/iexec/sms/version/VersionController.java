/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.version;

import com.iexec.sms.api.config.TeeServicesProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class VersionController {

    public static final String METRIC_INFO_GAUGE_NAME = "iexec.version.info";
    public static final String METRIC_INFO_GAUGE_DESC = "A metric to expose version and application name.";
    public static final String METRIC_INFO_LABEL_APP_NAME = "iexecAppName";
    public static final String METRIC_INFO_LABEL_APP_VERSION = "iexecAppVersion";
    public static final String METRIC_INFO_LABEL_TEE_FRAMEWORK = "teeFramework";
    // Must be static final to avoid garbage collect and side effect on gauge
    public static final int METRIC_VALUE = 1;
    private final BuildProperties buildProperties;
    private final Map<String, TeeServicesProperties> teeServicesPropertiesMap;

    public VersionController(BuildProperties buildProperties, Map<String, TeeServicesProperties> teeServicesPropertiesMap) {
        this.buildProperties = buildProperties;
        this.teeServicesPropertiesMap = teeServicesPropertiesMap;
    }

    @PostConstruct
    void initializeGaugeVersion() {
        Gauge.builder(METRIC_INFO_GAUGE_NAME, () -> METRIC_VALUE)
                .description(METRIC_INFO_GAUGE_DESC)
                .tags(METRIC_INFO_LABEL_APP_VERSION, buildProperties.getVersion(),
                        METRIC_INFO_LABEL_APP_NAME, buildProperties.getName(),
                        METRIC_INFO_LABEL_TEE_FRAMEWORK, teeServicesPropertiesMap.entrySet().iterator().next().getValue().getTeeFramework().name())
                .register(Metrics.globalRegistry);
    }

    @GetMapping("/version")
    public ResponseEntity<String> getVersion() {
        return ResponseEntity.ok(buildProperties.getVersion());
    }
}
