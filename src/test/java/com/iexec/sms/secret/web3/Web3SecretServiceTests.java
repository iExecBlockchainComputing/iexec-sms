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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @InjectMocks
    private Web3SecretService web3SecretService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldNotAddSecretIfPresent() {
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue, true);
        when(web3SecretRepository.find(secretAddress)).thenReturn(Optional.of(web3Secret));
        assertThat(web3SecretService.addSecret(secretAddress, plainSecretValue)).isFalse();
        verifyNoInteractions(encryptionService);
        verify(web3SecretRepository, never()).save(any());
    }

    @Test
    void shouldAddSecret() {
        when(web3SecretRepository.find(secretAddress)).thenReturn(Optional.empty());
        when(encryptionService.encrypt(plainSecretValue)).thenReturn(encryptedSecretValue);
        assertThat(web3SecretService.addSecret(secretAddress, plainSecretValue)).isTrue();
        verify(encryptionService).encrypt(any());
        verify(web3SecretRepository).save(any());
    }

    @Test
    void shouldGetDecryptedSecret() {
        Web3Secret encryptedSecret = new Web3Secret(secretAddress, encryptedSecretValue, true);
        when(web3SecretRepository.find(secretAddress)).thenReturn(Optional.of(encryptedSecret));
        when(encryptionService.decrypt(encryptedSecretValue)).thenReturn(plainSecretValue);

        Optional<Web3Secret> result = web3SecretService.getSecret(secretAddress, true);
        assertThat(result).isPresent();
        assertThat(result).get().extracting(Web3Secret::getHeader).usingRecursiveComparison().isEqualTo(new Web3SecretHeader(secretAddress));
        assertThat(result).get().extracting(Web3Secret::getValue).isEqualTo(plainSecretValue);
        assertThat(result).get().extracting(Web3Secret::isEncryptedValue).isEqualTo(false);

        verify(web3SecretRepository).find(secretAddress);
        verify(encryptionService).decrypt(any());
    }

    @Test
    void shouldGetEncryptedSecret() {
        Web3Secret encryptedSecret = new Web3Secret(secretAddress, encryptedSecretValue, true);
        when(web3SecretRepository.find(secretAddress)).thenReturn(Optional.of(encryptedSecret));
        Optional<Web3Secret> oSecret1 = web3SecretService.getSecret(secretAddress, false);
        Optional<Web3Secret> oSecret2 = web3SecretService.getSecret(secretAddress);
        assertThat(oSecret1)
                .contains(encryptedSecret)
                .isEqualTo(oSecret2);
        verify(web3SecretRepository, times(2)).find(secretAddress);
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldGetEmptyResultIfSecretNotPresent() {
        when(web3SecretRepository.find(secretAddress)).thenReturn(Optional.empty());
        assertThat(web3SecretService.getSecret(secretAddress)).isEmpty();
        assertThat(web3SecretService.getSecret(secretAddress, false)).isEmpty();
        verify(web3SecretRepository, times(2)).find(secretAddress);
        verifyNoInteractions(encryptionService);
    }

}