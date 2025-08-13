/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.scone;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

class SconeSessionSecurityConfigTests {

    private Validator validator;

    private static final URL MAA_URL;

    static {
        try {
            MAA_URL = new URL("https://maa.attestation.service");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldValidateHardwareConfig() {
        final SconeSessionSecurityConfig sessionSecurityConfig = new SconeSessionSecurityConfig(
                List.of(), List.of(), "hardware", null, List.of());
        final Set<ConstraintViolation<SconeSessionSecurityConfig>> violations = validator.validate(sessionSecurityConfig);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldNotValidateMaaConfigWithNoList() {
        assertThatException()
                .isThrownBy(() -> new SconeSessionSecurityConfig(List.of(), List.of(), "maa", null, null))
                .withMessage("Attestation URL can not be null when scone session mode is 'maa'");
    }

    @Test
    void shouldNotValidateMaaConfigWithEmptyList() {
        assertThatException()
                .isThrownBy(() -> new SconeSessionSecurityConfig(List.of(), List.of(), "maa", null, List.of()))
                .withMessage("Attestation URL can not be null when scone session mode is 'maa'");
    }

    @Test
    void shouldValidateMaaConfigWithOnlyUrl() {
        final SconeSessionSecurityConfig sessionSecurityConfig = new SconeSessionSecurityConfig(
                List.of(), List.of(), "maa", MAA_URL, List.of());
        final Set<ConstraintViolation<SconeSessionSecurityConfig>> violations = validator.validate(sessionSecurityConfig);
        assertThat(violations).isEmpty();
        assertThat(sessionSecurityConfig.getUrls()).isEqualTo(List.of(MAA_URL));
    }

    @Test
    void shouldValidateMaaConfigWithList() {
        final SconeSessionSecurityConfig sessionSecurityConfig = new SconeSessionSecurityConfig(
                List.of(), List.of(), "maa", null, List.of(MAA_URL));
        final Set<ConstraintViolation<SconeSessionSecurityConfig>> violations = validator.validate(sessionSecurityConfig);
        assertThat(violations).isEmpty();
        assertThat(sessionSecurityConfig.getUrls()).isEqualTo(List.of(MAA_URL));
    }
}
