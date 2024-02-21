/*
 * Copyright 2024-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.sms.admin.AdminService;
import com.iexec.sms.encryption.EncryptionConfiguration;
import com.iexec.sms.encryption.EncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class OufOfServiceFilterConfigTests {

    @TempDir
    public File tempDir;

    private final ApplicationContextRunner runner = new ApplicationContextRunner();

    @Test
    void shouldCreateOufOfServiceFilter() {
        runner.withPropertyValues("admin.out-of-service.enabled=true").withBean(EncryptionConfiguration.class, tempDir.getAbsolutePath() + "/aes.key")
                .withBean(EncryptionService.class)
                .withBean(AdminService.class)
                .withConfiguration(UserConfigurations.of(OufOfServiceFilterConfig.class))
                .run(context -> assertThat(context).hasSingleBean(OufOfServiceFilterConfig.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "admin.out-of-service.enabled", "admin.out-of-service.enabled=false", "admin.out-of-service.enabled=1"})
    void shouldNotCreateFilterWhenIsNotEnabled(String value) {
        runner.withPropertyValues(value).withBean(EncryptionConfiguration.class, tempDir.getAbsolutePath() + "/aes.key")
                .withBean(EncryptionService.class)
                .withBean(AdminService.class)
                .withConfiguration(UserConfigurations.of(OufOfServiceFilterConfig.class))
                .run(context -> assertThat(context).doesNotHaveBean(OufOfServiceFilterConfig.class));
    }
}
