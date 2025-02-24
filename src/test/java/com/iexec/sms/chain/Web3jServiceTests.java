/*
 * Copyright 2023-2025 IEXEC BLOCKCHAIN TECH
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
        "blockchain.id=134",
        "blockchain.is-sidechain=true",
        "blockchain.node-address=https://bellecour.iex.ec",
        "blockchain.hub-address=0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f",
        "blockchain.block-time=PT5S",
        "blockchain.gas-price-multiplier=1.0",
        "blockchain.gas-price-cap=22000000000"})
class Web3jServiceTests {
    @Autowired
    private ChainConfig chainConfig;

    @Test
    void checkChainConfig() {
        final ChainConfig expectedConfig = new ChainConfig(
                134, true, "https://bellecour.iex.ec",
                "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f", Duration.ofSeconds(5),
                1.0f, 22_000_000_000L
        );
        assertThat(chainConfig).isEqualTo(expectedConfig);
    }

    @Test
    void shouldCreateInstance() {
        assertThat(new Web3jService(chainConfig)).isNotNull();
    }
}
