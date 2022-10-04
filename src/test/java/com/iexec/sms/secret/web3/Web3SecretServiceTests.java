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
        Web3Secret web3Secret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3SecretRepository.findWeb3SecretByAddress(secretAddress)).thenReturn(Optional.of(web3Secret));
        assertThat(web3SecretService.addSecret(secretAddress, plainSecretValue)).isFalse();
        verifyNoInteractions(encryptionService);
        verify(web3SecretRepository, never()).save(any());
    }

    @Test
    void shouldAddSecret() {
        when(web3SecretRepository.findWeb3SecretByAddress(secretAddress)).thenReturn(Optional.empty());
        when(encryptionService.encrypt(plainSecretValue)).thenReturn(encryptedSecretValue);
        assertThat(web3SecretService.addSecret(secretAddress, plainSecretValue)).isTrue();
        verify(encryptionService).encrypt(any());
        verify(web3SecretRepository).save(any());
    }

    @Test
    void shouldGetDecryptedSecret() {
        Web3Secret encryptedSecret = new Web3Secret(secretAddress, encryptedSecretValue);
        encryptedSecret.setEncryptedValue(true);
        when(web3SecretRepository.findWeb3SecretByAddress(secretAddress)).thenReturn(Optional.of(encryptedSecret));
        when(encryptionService.decrypt(encryptedSecretValue)).thenReturn(plainSecretValue);
        Optional<Web3Secret> result = web3SecretService.getSecret(secretAddress, true);
        assertThat(result).contains(new Web3Secret(secretAddress, plainSecretValue));
        verify(web3SecretRepository).findWeb3SecretByAddress(secretAddress);
        verify(encryptionService).decrypt(any());
    }

    @Test
    void shouldGetEncryptedSecret() {
        Web3Secret encryptedSecret = new Web3Secret(secretAddress, encryptedSecretValue);
        when(web3SecretRepository.findWeb3SecretByAddress(secretAddress)).thenReturn(Optional.of(encryptedSecret));
        assertThat(web3SecretService.getSecret(secretAddress))
                .contains(encryptedSecret);
        verify(web3SecretRepository).findWeb3SecretByAddress(secretAddress);
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldGetEmptyResultIfSecretNotPresent() {
        when(web3SecretRepository.findWeb3SecretByAddress(secretAddress)).thenReturn(Optional.empty());
        assertThat(web3SecretService.getSecret(secretAddress)).isEmpty();
        verify(web3SecretRepository).findWeb3SecretByAddress(secretAddress);
        verifyNoInteractions(encryptionService);
    }

}