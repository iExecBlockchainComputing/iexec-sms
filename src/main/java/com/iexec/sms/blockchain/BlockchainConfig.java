/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BlockchainConfig {

    @Value("${blockchain.id}")
    private Integer chainId;

    @Value("${blockchain.node-address}")
    private String nodeAddress;

    @Value("${blockchain.hub-address}")
    private String hubAddress;

    @Value("${blockchain.block-time}")
    private Duration blockTime;

    @Value("${blockchain.gas-price-multiplier}")
    private float gasPriceMultiplier;

    @Value("${blockchain.gas-price-cap}")
    private long gasPriceCap;

    @Value("${blockchain.is-sidechain}")
    private boolean isSidechain;

}
