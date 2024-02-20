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
        runner.withPropertyValues("admin.outofservice.enabled=true").withBean(EncryptionConfiguration.class, tempDir.getAbsolutePath() + "/aes.key")
                .withBean(EncryptionService.class)
                .withBean(AdminService.class)
                .withConfiguration(UserConfigurations.of(OufOfServiceFilterConfig.class))
                .run(context -> assertThat(context).hasSingleBean(OufOfServiceFilterConfig.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "admin.outofservice.enabled", "admin.outofservice.enabled=false", "admin.outofservice.enabled=1"})
    void shouldNotCreateFilterWhenIsNotEnabled(String value) {
        runner.withPropertyValues(value).withBean(EncryptionConfiguration.class, tempDir.getAbsolutePath() + "/aes.key")
                .withBean(EncryptionService.class)
                .withBean(AdminService.class)
                .withConfiguration(UserConfigurations.of(OufOfServiceFilterConfig.class))
                .run(context -> assertThat(context).doesNotHaveBean(OufOfServiceFilterConfig.class));
    }
}
