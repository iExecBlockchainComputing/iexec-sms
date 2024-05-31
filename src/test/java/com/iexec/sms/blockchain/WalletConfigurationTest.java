/*
 * Copyright 2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.chain.SignerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class WalletConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner();

    @Test
    void shouldCreateBeans() {
        runner.withBean(BlockchainConfig.class, 65535, true, "http://localhost:8545", "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca", Duration.ofSeconds(5L), 1.0f, 0L)
                .withBean(IexecHubService.class)
                .withBean(Web3jService.class)
                .withConfiguration(UserConfigurations.of(WalletConfiguration.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(BlockchainConfig.class)
                        .hasSingleBean(IexecHubService.class)
                        .hasSingleBean(SignerService.class)
                        .hasSingleBean(WalletConfiguration.class)
                        .hasSingleBean(Web3jService.class));
    }
}
