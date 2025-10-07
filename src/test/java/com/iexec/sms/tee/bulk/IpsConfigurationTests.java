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

package com.iexec.sms.tee.bulk;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IpsConfigurationTests {
    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "")
    void shouldNotBeValidWithEmptyValues(final String gatewayUrl) {
        final IpfsConfiguration config = new IpfsConfiguration(gatewayUrl);
        assertThat(validator.validate(config))
                .isNotEmpty()
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "not-an-url"})
    void shouldNotBeValidWithInvalidUrls(final String gatewayUrl) {
        final IpfsConfiguration config = new IpfsConfiguration(gatewayUrl);
        assertThat(validator.validate(config))
                .isNotEmpty()
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("must be a valid URL");
    }

    @Test
    void shouldBeValid() {
        final IpfsConfiguration config = new IpfsConfiguration("http://localhost");
        assertThat(validator.validate(config)).isEmpty();
    }
}
