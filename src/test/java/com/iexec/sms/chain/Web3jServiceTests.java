/*
 * Copyright 2023-2026 IEXEC BLOCKCHAIN TECH
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = ChainConfig.class)
@TestPropertySource(properties = {
        "chain.id=421614",
        "chain.sidechain=false",
        "chain.node-address=https://sepolia-rollup.arbitrum.io/rpc",
        "chain.hub-address=0xB2157BF2fAb286b2A4170E3491Ac39770111Da3E",
        "chain.block-time=PT5S",
        "chain.gas-price-multiplier=1.1",
        "chain.gas-price-cap=22000000000"})
class Web3jServiceTests {
    @Autowired
    private ChainConfig chainConfig;

    @Test
    void checkChainConfig() {
        final ChainConfig expectedConfig = new ChainConfig(
                421614, false, "https://sepolia-rollup.arbitrum.io/rpc",
                "0xB2157BF2fAb286b2A4170E3491Ac39770111Da3E", Duration.ofSeconds(5),
                1.1f, 22_000_000_000L
        );
        assertThat(chainConfig).isEqualTo(expectedConfig);
    }

    @Test
    void shouldCreateInstance() {
        assertThat(new Web3jService(chainConfig)).isNotNull();
    }
}
