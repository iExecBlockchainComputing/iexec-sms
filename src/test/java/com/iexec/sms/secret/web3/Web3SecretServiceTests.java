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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.sms.MemoryLogAppender;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Web3SecretServiceTests {
    String secretAddress = "secretAddress".toLowerCase();
    String plainSecretValue = "plainSecretValue";
    String encryptedSecretValue = "encryptedSecretValue";

    @Mock
    private EncryptionService encryptionService;
    @Mock
    private Web3SecretRepository web3SecretRepository;
    @Mock
    private MeasuredSecretService measuredSecretService;
    @InjectMocks
    private Web3SecretService web3SecretService;
    private static MemoryLogAppender memoryLogAppender;

    @BeforeAll
    static void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.iexec.sms.secret");
        memoryLogAppender = new MemoryLogAppender();
        memoryLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryLogAppender);
        memoryLogAppender.start();
    }

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        memoryLogAppender.reset();
    }

    // region addSecret
    @Test
    void shouldNotAddSecretIfPresent() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.of(web3Secret));
        assertThat(web3SecretService.addSecret(secretAddress, plainSecretValue)).isFalse();
        verify(measuredSecretService, times(0)).newlyAddedSecret();
        verifyNoInteractions(encryptionService);
        verify(web3SecretRepository, never()).save(any());
    }

    @Test
    void shouldAddSecret() {
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.empty());
        when(encryptionService.encrypt(plainSecretValue)).thenReturn(encryptedSecretValue);
        when(web3SecretRepository.save(any())).thenReturn(new Web3Secret(secretAddress, encryptedSecretValue));

        final boolean success = web3SecretService.addSecret(secretAddress, plainSecretValue);
        assertAll(
                () -> assertTrue(success),
                () -> verify(measuredSecretService).newlyAddedSecret(),
                () -> verify(encryptionService).encrypt(any()),
                () -> verify(web3SecretRepository).save(any()),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
        );
    }
    // endregion


    // region isSecretPresent
    @Test
    void shouldGetSecretExistFromDBAndPutInCache() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.of(web3Secret));

        final boolean resultFirstCall = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertTrue(resultFirstCall),
                () -> verify(web3SecretRepository, times(1)).findById(any()),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was not found in cache")),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
        );
    }

    @Test
    void shouldGetSecretExistFromCache() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.of(web3Secret));

        web3SecretService.isSecretPresent(secretAddress);
        memoryLogAppender.reset();
        boolean resultSecondCall = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertTrue(resultSecondCall),
                //put 1 bellow means no new invocation since 1st call
                () -> verify(web3SecretRepository, times(1)).findById(any()),
                () -> assertTrue(memoryLogAppender.doesNotContains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache")),
                () -> assertTrue(memoryLogAppender.contains("exist:true"))
        );
    }

    @Test
    void shouldGetSecretNotExistFromCache() {
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.empty());

        web3SecretService.isSecretPresent(secretAddress);
        memoryLogAppender.reset();
        boolean resultSecondCall = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertFalse(resultSecondCall),
                //put 1 bellow means no new invocation since 1st call
                () -> verify(web3SecretRepository, times(1)).findById(any()),
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
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.of(encryptedSecret));
        when(encryptionService.decrypt(encryptedSecretValue)).thenReturn(plainSecretValue);

        Optional<String> result = web3SecretService.getDecryptedValue(secretAddress);
        assertThat(result)
                .isPresent()
                .get().isEqualTo(plainSecretValue);

        verify(web3SecretRepository).findById(any(Web3SecretHeader.class));
        verify(encryptionService).decrypt(any());
    }

    @Test
    void shouldGetEmptyValueIfSecretNotPresent() {
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.empty());
        assertThat(web3SecretService.getDecryptedValue(secretAddress)).isEmpty();
        verify(web3SecretRepository, times(1)).findById(any(Web3SecretHeader.class));
        verifyNoInteractions(encryptionService);
    }
    // endregion

    // region getSecret
    @Test
    void shouldGetEncryptedSecret() {
        Web3Secret encryptedSecret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.of(encryptedSecret));
        Optional<Web3Secret> result = web3SecretService.getSecret(secretAddress);
        assertThat(result)
                .contains(encryptedSecret);
        verify(web3SecretRepository, times(1)).findById(any(Web3SecretHeader.class));
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldGetEmptySecretIfSecretNotPresent() {
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.empty());
        assertThat(web3SecretService.getSecret(secretAddress)).isEmpty();
        verify(web3SecretRepository, times(1)).findById(any(Web3SecretHeader.class));
        verifyNoInteractions(encryptionService);
    }
    // endregion
}
