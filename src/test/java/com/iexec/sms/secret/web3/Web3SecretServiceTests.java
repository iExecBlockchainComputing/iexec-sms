/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.web3;

import ch.qos.logback.classic.Logger;
import com.iexec.sms.MemoryLogAppender;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.CacheSecretService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Web3SecretServiceTests {
    String secretAddress = "secretAddress".toLowerCase();
    String plainSecretValue = "plainSecretValue";
    String encryptedSecretValue = "encryptedSecretValue";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Web3SecretRepository web3SecretRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private MeasuredSecretService measuredSecretService;

    private CacheSecretService<Web3SecretHeader> web3CacheSecretService;

    private Web3SecretService web3SecretService;

    private static MemoryLogAppender memoryLogAppender;

    @BeforeAll
    void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.iexec.sms.secret");
        memoryLogAppender = (MemoryLogAppender) logger.getAppender("MEM");
        web3CacheSecretService = new CacheSecretService<>();
    }

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        memoryLogAppender.reset();
        web3SecretRepository.deleteAll();
        web3CacheSecretService.clear();
        web3SecretService = new Web3SecretService(
                jdbcTemplate, web3SecretRepository, encryptionService, measuredSecretService, web3CacheSecretService);
    }

    // region addSecret
    @Test
    void shouldNotAddSecretIfPresent() {
        when(encryptionService.encrypt(plainSecretValue)).thenReturn(encryptedSecretValue);
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        web3SecretRepository.saveAndFlush(web3Secret);
        assertThat(web3SecretService.addSecret(secretAddress, plainSecretValue)).isFalse();
        verify(measuredSecretService, times(0)).newlyAddedSecret();
        verify(encryptionService).encrypt(plainSecretValue);
        assertThat(web3SecretRepository.count()).isOne();
    }

    @Test
    void shouldAddSecret() {
        when(encryptionService.encrypt(plainSecretValue)).thenReturn(encryptedSecretValue);

        final boolean success = web3SecretService.addSecret(secretAddress, plainSecretValue);
        assertAll(
                () -> assertTrue(success),
                () -> verify(measuredSecretService).newlyAddedSecret(),
                () -> verify(encryptionService).encrypt(any()),
                () -> assertThat(web3SecretRepository.count()).isOne(),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
        );
    }
    // endregion


    // region isSecretPresent
    @Test
    void shouldGetSecretExistFromDBAndPutInCache() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        web3SecretRepository.save(web3Secret);

        final boolean resultFirstCall = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertTrue(resultFirstCall),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was not found in cache")),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
        );
    }

    @Test
    void shouldGetSecretExistFromCache() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        web3SecretRepository.save(web3Secret);

        web3SecretService.isSecretPresent(secretAddress);
        memoryLogAppender.reset();
        boolean resultSecondCall = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertTrue(resultSecondCall),
                () -> assertTrue(memoryLogAppender.doesNotContains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache")),
                () -> assertTrue(memoryLogAppender.contains("exist:true"))
        );
    }

    @Test
    void shouldGetSecretNotExistFromCache() {
        web3SecretService.isSecretPresent(secretAddress);
        memoryLogAppender.reset();
        boolean resultSecondCall = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertFalse(resultSecondCall),
                () -> assertTrue(memoryLogAppender.doesNotContains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache")),
                () -> assertTrue(memoryLogAppender.contains("exist:false"))
        );
    }
    // endregion

    // region getDecryptedValue
    @Test
    void shouldGetDecryptedValue() {
        Web3Secret encryptedSecret = new Web3Secret(secretAddress, encryptedSecretValue);
        web3SecretRepository.save(encryptedSecret);
        when(encryptionService.decrypt(encryptedSecretValue)).thenReturn(plainSecretValue);

        Optional<String> result = web3SecretService.getDecryptedValue(secretAddress);
        assertThat(result).contains(plainSecretValue);

        verify(encryptionService).decrypt(any());
    }

    @Test
    void shouldGetEmptyValueIfSecretNotPresent() {
        assertThat(web3SecretService.getDecryptedValue(secretAddress)).isEmpty();
        verifyNoInteractions(encryptionService);
    }
    // endregion

    // region getSecret
    @Test
    void shouldGetEncryptedSecret() {
        Web3Secret encryptedSecret = new Web3Secret(secretAddress, encryptedSecretValue);
        web3SecretRepository.save(encryptedSecret);
        Optional<Web3Secret> result = web3SecretService.getSecret(secretAddress);
        assertThat(result)
                .isNotEmpty()
                .usingRecursiveComparison()
                .isEqualTo(Optional.of(encryptedSecret));
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldGetEmptySecretIfSecretNotPresent() {
        assertThat(web3SecretService.getSecret(secretAddress)).isEmpty();
        verifyNoInteractions(encryptionService);
    }
    // endregion
}
