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

package com.iexec.sms.secret.web3;

import com.iexec.sms.encryption.EncryptionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Web3SecretServiceTests {
    private static final String METRICS_PREFIX = "iexec.sms.secrets.web3.";

    String secretAddress = "secretAddress".toLowerCase();
    String plainSecretValue = "plainSecretValue";
    String encryptedSecretValue = "encryptedSecretValue";

    @Mock
    private EncryptionService encryptionService;
    @Mock
    private Web3SecretRepository web3SecretRepository;
    @InjectMocks
    private Web3SecretService web3SecretService;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void beforeEach() {
        meterRegistry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(meterRegistry);

        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void afterEach() {
        meterRegistry.clear();
        Metrics.globalRegistry.clear();
    }

    // region init
    @Test
    void shouldRegisterCounter() {
        final long initialCount = 5L;
        when(web3SecretRepository.count()).thenReturn(initialCount);

        web3SecretService.init();

        assertInitialCount(initialCount);
    }
    // endregion

    // region addSecret
    @Test
    void shouldNotAddSecretIfPresent() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.of(web3Secret));
        assertThat(web3SecretService.addSecret(secretAddress, plainSecretValue)).isFalse();
        assertAddedCount(0);
        verifyNoInteractions(encryptionService);
        verify(web3SecretRepository, never()).save(any());
    }

    @Test
    void shouldAddSecret() {
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.empty());
        when(encryptionService.encrypt(plainSecretValue)).thenReturn(encryptedSecretValue);
        assertThat(web3SecretService.addSecret(secretAddress, plainSecretValue)).isTrue();
        assertAddedCount(1);
        verify(encryptionService).encrypt(any());
        verify(web3SecretRepository).save(any());
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

    // region stored count
    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 10, Long.MAX_VALUE})
    void storedCount(long expectedCount) {
        when(web3SecretRepository.count()).thenReturn(expectedCount);

        assertStoredCount(expectedCount);

        verify(web3SecretRepository).count();
    }
    // endregion

    // region utils
    void assertInitialCount(long expectedCount) {
        final Counter initialCounter = meterRegistry.find(METRICS_PREFIX + "initial")
                .counter();

        assertThat(initialCounter)
                .extracting(Counter::count)
                .isEqualTo((double) expectedCount);
    }

    void assertAddedCount(long expectedCount) {
        final Counter addedCounter = meterRegistry.find(METRICS_PREFIX + "added")
                .counter();

        assertThat(addedCounter)
                .extracting(Counter::count)
                .isEqualTo((double) expectedCount);
    }

    void assertStoredCount(long expectedCount) {
        final Gauge storedGauge = meterRegistry.find(METRICS_PREFIX + "stored")
                .gauge();

        assertThat(storedGauge)
                .extracting(Gauge::value)
                .isEqualTo((double) expectedCount);
    }
    // endregion
}
