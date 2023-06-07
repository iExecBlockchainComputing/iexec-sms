/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.blockchain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = BlockchainConfig.class)
@TestPropertySource(properties = {
        "blockchain.id=134",
        "blockchain.is-sidechain=true",
        "blockchain.node-ddress=https://bellecour.iex.ec",
        "blockchain.hub-ddress=0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f",
        "blockchain.block-time=PT5S",
        "blockchain.gas-price-multiplier=1.0",
        "blockchain.gas-price-cap=22000000000" })
class Web3jServiceTests {
    @Autowired
    private BlockchainConfig blockchainConfig;

    @Test
    void shouldCreateInstance() {
        assertThat(new Web3jService(blockchainConfig)).isNotNull();
    }
}
