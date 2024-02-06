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

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.CacheSecretService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    @Mock
    private CacheSecretService<Web3SecretHeader> web3CacheSecretService;

    @InjectMocks
    private Web3SecretService web3SecretService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region addSecret
    @Test
    void shouldNotAddSecretIfPresent() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3CacheSecretService.lookSecretExistenceInCache(any(Web3SecretHeader.class))).thenReturn(null);

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
                () -> verify(web3SecretRepository).save(any())
        );
    }
    // endregion


    // region isSecretPresent
    @Test
    void shouldGetSecretExistFromDBAndPutInCache() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3CacheSecretService.lookSecretExistenceInCache(any(Web3SecretHeader.class))).thenReturn(null);
        when(web3SecretRepository.findById(any(Web3SecretHeader.class))).thenReturn(Optional.of(web3Secret));

        final boolean isSecretPresent = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertTrue(isSecretPresent),
                () -> verify(web3SecretRepository, times(1)).findById(any())
        );
    }

    @Test
    void shouldGetSecretExistFromCache() {
        when(web3CacheSecretService.lookSecretExistenceInCache(any(Web3SecretHeader.class))).thenReturn(true);

        boolean isSecretPresent = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertTrue(isSecretPresent),
                () -> verify(web3SecretRepository, times(0)).findById(any())
        );
    }

    @Test
    void shouldGetSecretNotExistFromCache() {
        when(web3CacheSecretService.lookSecretExistenceInCache(any(Web3SecretHeader.class))).thenReturn(false);

        boolean isSecretPresent = web3SecretService.isSecretPresent(secretAddress);
        assertAll(
                () -> assertFalse(isSecretPresent),
                () -> verify(web3SecretRepository, times(0)).findById(any())
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
