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

package com.iexec.sms.chain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChainConfigTests {
    
    private static final String HUB_ADDRESS = "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f";
    private static final String NODE_ADDRESS = "https://bellecour.iex.ec";
    
    private Validator validator;

    @BeforeEach
    void setUp() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validConfigShouldPassValidation() {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                Duration.ofSeconds(5),
                1.0f,
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).isEmpty();
    }

    @Test
    void invalidIdShouldFailValidation() {
        final ChainConfig config = new ChainConfig(
                0, // Invalid: should be positive
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                Duration.ofSeconds(5),
                1.0f,
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("id");
    }

    @Test
    void emptyNodeAddressShouldFailValidation() {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                "", // Invalid: should not be empty
                HUB_ADDRESS,
                Duration.ofSeconds(5),
                1.0f,
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("nodeAddress");
    }

    @Test
    void invalidUrlFormatShouldFailValidation() {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                "not-a-url", // Invalid URL format
                HUB_ADDRESS,
                Duration.ofSeconds(5),
                1.0f,
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("nodeAddress");
    }

    @Test
    void invalidEthereumAddressShouldFailValidation() {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                NODE_ADDRESS,
                "0x0000000000000000000000000000000000000000", // Zero address
                Duration.ofSeconds(5),
                1.0f,
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("hubAddress");
    }

    @ParameterizedTest
    @ValueSource(longs = {50, 25000}) // Too short and too long durations
    void invalidBlockTimeShouldFailValidation(long millis) {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                Duration.ofMillis(millis),
                1.0f,
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("blockTime");
    }

    @Test
    void nullBlockTimeShouldFailValidation() {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                null, // Invalid: cannot be null
                1.0f,
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("blockTime");
    }

    @Test
    void negativeGasPriceMultiplierShouldFailValidation() {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                Duration.ofSeconds(5),
                -0.5f, // Invalid: should be positive
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("gasPriceMultiplier");
    }

    @Test
    void negativeBlockTimeShouldFailValidation() {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                Duration.ofMillis(-1000), // Negative duration
                1.0f,
                22_000_000_000L
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("blockTime");
    }

    @Test
    void negativeGasPriceCapShouldFailValidation() {
        final ChainConfig config = new ChainConfig(
                134,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                Duration.ofSeconds(5),
                1.0f,
                -1L // Invalid: should be positive or zero
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("gasPriceCap");
    }

    @Test
    void multipleViolationsShouldBeReported() {
        final ChainConfig config = new ChainConfig(
                0, // Invalid
                true,
                "", // Invalid
                "0x0000000000000000000000000000000000000000", // Invalid
                Duration.ofMillis(50), // Invalid
                -1.0f, // Invalid
                -1L // Invalid
        );
        final Set<ConstraintViolation<ChainConfig>> violations = validator.validate(config);
        assertThat(violations).hasSize(6); // All fields except sidechain have violations
    }
}
