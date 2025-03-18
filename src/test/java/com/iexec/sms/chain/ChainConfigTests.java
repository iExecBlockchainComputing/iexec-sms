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
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class ChainConfigTests {

    private static final int CHAIN_ID = 1;
    private static final String HUB_ADDRESS = "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f";
    private static final String NODE_ADDRESS = "https://bellecour.iex.ec";
    private static final Duration BLOCK_TIME = Duration.ofSeconds(1);
    private static final float GAS_PRICE_MULTIPLIER = 1.0f;
    private static final long GAS_PRICE_CAP = 22_000_000_000L;

    private Set<ConstraintViolation<ChainConfig>> validate(ChainConfig chainConfig) {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(chainConfig);
        }
    }

    // region Valid data
    static Stream<Arguments> validData() {
        return Stream.of(
                Arguments.of(100, true, "http://localhost:8545", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT0.1S", 1.0f, 11_000_000_000L),
                Arguments.of(42, true, "https://localhost:8545", "0x0000000000000000000000000000000000000001", "PT10S", 1.0f, 22_000_000_000L),
                Arguments.of(10, true, "https://www.classic-url.com", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT20S", 1.0f, 22_000_000_000L),
                Arguments.of(1, true, "http://ibaa.iex.ec:443/test?validation=should:be@OK", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT5S", 1.0f, 0L),
                Arguments.of(CHAIN_ID, true, NODE_ADDRESS, HUB_ADDRESS, BLOCK_TIME, GAS_PRICE_MULTIPLIER, GAS_PRICE_CAP)
        );
    }

    @ParameterizedTest
    @MethodSource("validData")
    void shouldValidate(int chainId,
                        boolean sidechain,
                        String nodeAddress,
                        String hubAddress,
                        Duration blockTime,
                        float gasPriceMultiplier,
                        long gasPriceCap) {
        final ChainConfig chainConfig = new ChainConfig(
                chainId,
                sidechain,
                nodeAddress,
                hubAddress,
                blockTime,
                gasPriceMultiplier,
                gasPriceCap);
        assertThat(validate(chainConfig)).isEmpty();
    }
    // endregion

    // region Invalid chain ids
    static Stream<Integer> invalidChainIds() {
        return Stream.of(
                0,      // Chain id should be strictly positive
                -1      // Chain id should be strictly positive
        );
    }

    @ParameterizedTest
    @MethodSource("invalidChainIds")
    void shouldNotValidateChainId(int chainId) {
        final ChainConfig chainConfig = new ChainConfig(
                chainId,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                BLOCK_TIME,
                GAS_PRICE_MULTIPLIER,
                GAS_PRICE_CAP);
        assertThat(validate(chainConfig))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Chain id must be greater than 0");
    }
    // endregion

    // region Invalid node addresses
    static Stream<Arguments> invalidNodeAddresses() {
        return Stream.of(
            Arguments.of(null, "Node address must not be empty"),       // Node address should not be null
            Arguments.of("", "Node address must not be empty"),         // Node address should be a valid URL
            Arguments.of("12345", "Node address must be a valid URL"),   // Node address should be a valid URL
            Arguments.of("0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "Node address must be a valid URL")    // Node address should be a valid URL
        );
    }

    @ParameterizedTest
    @MethodSource("invalidNodeAddresses")
    void shouldNotValidateNodeAddress(String nodeAddress, String errorMessage) {
        final ChainConfig chainConfig = new ChainConfig(
                CHAIN_ID,
                true,
                nodeAddress,
                HUB_ADDRESS,
                BLOCK_TIME,
                GAS_PRICE_MULTIPLIER,
                GAS_PRICE_CAP);
        assertThat(validate(chainConfig))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(errorMessage);
    }
    // endregion

    // region Invalid hub address
    static Stream<String> invalidHubAddresses() {
        return Stream.of(
                null,       // Hub address should not be null
                "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd200211111111111111", // Hub address size should be exactly 40
                "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd200",    // Hub address size should be exactly 40
                "0x0000000000000000000000000000000000000000",    // Hub address should not be zero
                "http://hub.address"   // Hub address should be an Ethereum address
        );
    }

    @ParameterizedTest
    @MethodSource("invalidHubAddresses")
    void shouldNotValidateHubAddress(String hubAddress) {
        final ChainConfig chainConfig = new ChainConfig(
                CHAIN_ID,
                true,
                NODE_ADDRESS,
                hubAddress,
                BLOCK_TIME,
                GAS_PRICE_MULTIPLIER,
                GAS_PRICE_CAP);
        assertThat(validate(chainConfig))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Hub address must be a valid non zero Ethereum address");
    }
    // endregion

    // region Invalid block time
    static Stream<Arguments> invalidBlockTimes() {
        return Stream.of(
                Arguments.of(Duration.ofSeconds(0), "Block time must be greater than 100ms"),
                Arguments.of(Duration.ofSeconds(25), "Block time must be less than 20s"),
                Arguments.of(Duration.ofSeconds(-1), "Block time must be greater than 100ms"),
                Arguments.of(null,  "Block time must not be null")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBlockTimes")
    void shouldNotValidateBlockTime(Duration blockTime, String errorMessage) {
        final ChainConfig chainConfig = new ChainConfig(
                CHAIN_ID,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                blockTime,
                GAS_PRICE_MULTIPLIER,
                GAS_PRICE_CAP);
        assertThat(validate(chainConfig))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(errorMessage);
    }
    // endregion

    // region Invalid gas price multiplier
    static Stream<Float> invalidGasPriceMultiplier() {
        return Stream.of(
                0f, // Gas Price Multiplier should be strictly positive
                -0.5f // Gas Price Multiplier should be strictly positive
        );
    }

    @ParameterizedTest
    @MethodSource("invalidGasPriceMultiplier")
    void shouldNotValidateGasPriceMultiplier(Float gasPriceMultiplier) {
        final ChainConfig chainConfig = new ChainConfig(
                CHAIN_ID,
                true,
                NODE_ADDRESS,
                HUB_ADDRESS,
                BLOCK_TIME,
                gasPriceMultiplier,
                GAS_PRICE_CAP);
        final Set<ConstraintViolation<ChainConfig>> violations = validate(chainConfig);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Gas price multiplier must be greater than 0");
    }
    // endregion

    // region Invalid gas price cap
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
        final Set<ConstraintViolation<ChainConfig>> violations = validate(config);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Gas price cap must be greater than or equal to 0");
    }
    // endregion

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
        final Set<ConstraintViolation<ChainConfig>> violations = validate(config);
        assertThat(violations).hasSize(6); // All fields except sidechain have violations
    }

}
