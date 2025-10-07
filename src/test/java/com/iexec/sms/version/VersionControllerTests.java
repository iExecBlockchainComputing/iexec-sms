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

import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.config.GramineServicesProperties;
import com.iexec.sms.api.config.SconeServicesProperties;
import com.iexec.sms.api.config.TeeAppProperties;
import com.iexec.sms.api.config.TeeServicesProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@Import(ProjectInfoAutoConfiguration.class)
class VersionControllerTests {
    private final String teeFrameworkVersion = "v5";
    TeeAppProperties preComputeProperties;
    TeeAppProperties postComputeProperties;
    private VersionController versionController;
    @Autowired
    private BuildProperties buildProperties;

    @BeforeAll
    static void initRegistry() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @AfterEach
    void afterEach() {
        Metrics.globalRegistry.clear();
    }

    @Test
    void testVersionController() {
        TeeServicesProperties properties = new SconeServicesProperties(
                teeFrameworkVersion,
                preComputeProperties,
                postComputeProperties,
                "lasImage"
        );
        final Map<String, TeeServicesProperties> sconePropertiesMap = Map.of(teeFrameworkVersion, properties);
        versionController = new VersionController(buildProperties, sconePropertiesMap);
        assertEquals(ResponseEntity.ok(buildProperties.getVersion()), versionController.getVersion());
    }

    @ParameterizedTest
    @EnumSource(value = TeeFramework.class, names = {"SCONE", "GRAMINE"})
    void shouldReturnInfoGauge(TeeFramework teeFramework) {
        TeeServicesProperties properties;
        if (teeFramework == TeeFramework.SCONE) {
            properties = new SconeServicesProperties(
                    teeFrameworkVersion,
                    preComputeProperties,
                    postComputeProperties,
                    "lasImage"
            );
        } else {
            properties = new GramineServicesProperties(
                    teeFrameworkVersion,
                    preComputeProperties,
                    postComputeProperties
            );
        }
        final Map<String, TeeServicesProperties> sconePropertiesMap = Map.of(teeFrameworkVersion, properties);
        versionController = new VersionController(buildProperties, sconePropertiesMap);
        versionController.initializeGaugeVersion();

        final Gauge info = Metrics.globalRegistry.find(VersionController.METRIC_INFO_GAUGE_NAME).gauge();
        assertThat(info)
                .isNotNull()
                .extracting(Gauge::getId)
                .isNotNull()
                .extracting(
                        id -> id.getTag(VersionController.METRIC_INFO_LABEL_APP_NAME),
                        id -> id.getTag(VersionController.METRIC_INFO_LABEL_APP_VERSION),
                        id -> id.getTag(VersionController.METRIC_INFO_LABEL_TEE_FRAMEWORK)
                )
                .containsExactly(buildProperties.getName(), buildProperties.getVersion(), teeFramework.name());
        assertThat(info.value()).isEqualTo(VersionController.METRIC_VALUE);
    }
}
