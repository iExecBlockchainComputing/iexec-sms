/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BlockchainConfig {

    @Value("${blockchain.id}")
    private Integer chainId;

    @Value("${blockchain.nodeAddress}")
    private String nodeAddress;

    @Value("${blockchain.hubAddress}")
    private String hubAddress;

    @Value("${blockchain.gasPriceMultiplier}")
    private float gasPriceMultiplier;

    @Value("${blockchain.gasPriceCap}")
    private long gasPriceCap;

    @Value("${blockchain.isSidechain}")
    private boolean isSidechain;

}
