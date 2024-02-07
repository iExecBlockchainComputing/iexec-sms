/*
 * Copyright 2024-2024 IEXEC BLOCKCHAIN TECH
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
package com.iexec.sms.secret.compute;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TeeTaskComputeSecretTest {

    private final TeeTaskComputeSecretRepository teeTaskComputeSecretRepository;

    @TestConfiguration
    public static class TeeTaskComputeSecretTestConfiguration {
        @Bean
        public DataSource dataSource() {
            return DataSourceBuilder.create()
                    .url("jdbc:sqlite::memory:")
                    .username("sa")
                    .password("sa")
                    .build();
        }
    }

    TeeTaskComputeSecretTest(@Autowired TeeTaskComputeSecretRepository teeTaskComputeSecretRepository) {
        this.teeTaskComputeSecretRepository = teeTaskComputeSecretRepository;
    }

    private TeeTaskComputeSecret getAppDeveloperSecret() {
        return TeeTaskComputeSecret.builder()
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .onChainObjectAddress("0x1")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .fixedSecretOwner("")
                .key("0")
                .value("secretValue")
                .build();
    }

    private TeeTaskComputeSecret getRequesterSecret() {
        return TeeTaskComputeSecret.builder()
                .secretOwnerRole(SecretOwnerRole.REQUESTER)
                .onChainObjectAddress("")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .fixedSecretOwner("0x1")
                .key("secret-key")
                .value("secretValue")
                .build();
    }

    @Test
    void shouldNotSaveSecretWhenValueIsNull() {
        TeeTaskComputeSecret secret = TeeTaskComputeSecret.builder()
                .onChainObjectAddress("")
                .onChainObjectType(OnChainObjectType.APPLICATION)
                .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
                .fixedSecretOwner("")
                .key("key")
                //.value("")
                .build();
        assertThatThrownBy(() -> teeTaskComputeSecretRepository.saveAndFlush(secret))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldSaveAppDeveloperAndRequesterSecrets() {
        TeeTaskComputeSecret appDeveloperSecret = getAppDeveloperSecret();
        teeTaskComputeSecretRepository.save(appDeveloperSecret);
        final TeeTaskComputeSecretHeader appDeveloperSecretHeader = appDeveloperSecret.getHeader();
        TeeTaskComputeSecret requesterSecret = getRequesterSecret();
        teeTaskComputeSecretRepository.save(requesterSecret);
        final TeeTaskComputeSecretHeader requesterSecretHeader = requesterSecret.getHeader();
        assertThat(teeTaskComputeSecretRepository.count()).isEqualTo(2);
        assertThat(teeTaskComputeSecretRepository.getReferenceById(appDeveloperSecretHeader))
                .isEqualTo(appDeveloperSecret);
        assertThat(teeTaskComputeSecretRepository.getReferenceById(requesterSecretHeader))
                .isEqualTo(requesterSecret);
    }

}
