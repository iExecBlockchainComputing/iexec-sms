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

package com.iexec.sms.secret.web2;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class Web2SecretsServiceTests {

    String ownerAddress = "ownerAddress".toLowerCase();
    String secretAddress = "secretAddress";
    String plainSecretValue = "plainSecretValue";
    String encryptedSecretValue = "encryptedSecretValue";

    @Mock
    private Web2SecretsRepository web2SecretsRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private Web2SecretsService web2SecretsService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldGetDecryptedSecret() {
        Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue);
        encryptedSecret.setEncryptedValue(true);
        List<Secret> secretList = List.of(encryptedSecret);
        Web2Secrets web2SecretsMock = new Web2Secrets(ownerAddress);
        web2SecretsMock.setSecrets(secretList);
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress))
                .thenReturn(Optional.of(web2SecretsMock));
        when(encryptionService.decrypt(encryptedSecret.getValue()))
                .thenReturn(plainSecretValue);

        Optional<Secret> result = web2SecretsService.getSecret(ownerAddress, secretAddress, true);
        assertThat(result).isNotEmpty();
        assertThat(result.get().getAddress()).isEqualTo(secretAddress);
        assertThat(result.get().getValue()).isEqualTo(plainSecretValue);
    }

    @Test
    void shouldGetEncryptedSecret() {
        Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue);
        encryptedSecret.setEncryptedValue(true);
        List<Secret> secretList = List.of(encryptedSecret);
        Web2Secrets web2Secrets = new Web2Secrets(ownerAddress);
        web2Secrets.setSecrets(secretList);
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress))
                .thenReturn(Optional.of(web2Secrets));

        Optional<Secret> oSecret1 = web2SecretsService.getSecret(ownerAddress, secretAddress);
        Optional<Secret> oSecret2 = web2SecretsService.getSecret(ownerAddress, secretAddress, false);
        assertThat(oSecret1)
                .contains(encryptedSecret)
                .isEqualTo(oSecret2);
        verify(web2SecretsRepository, times(2)).findWeb2SecretsByOwnerAddress(ownerAddress);
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldGetEmptyResultIfSecretNotPresent() {
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress)).thenReturn(Optional.empty());
        assertThat(web2SecretsService.getSecret(ownerAddress, secretAddress)).isEmpty();
        assertThat(web2SecretsService.getSecret(ownerAddress, secretAddress, false)).isEmpty();
        verify(web2SecretsRepository, times(2)).findWeb2SecretsByOwnerAddress(ownerAddress);
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldNotAddSecretIfPresent() {
        Web2Secrets web2Secrets = new Web2Secrets(ownerAddress);
        web2Secrets.getSecrets().add(new Secret(secretAddress, encryptedSecretValue));
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress)).thenReturn(Optional.of(web2Secrets));
        assertThat(web2SecretsService.addSecret(ownerAddress, secretAddress, plainSecretValue)).isFalse();
        verifyNoInteractions(encryptionService);
        verify(web2SecretsRepository, never()).save(any());
    }

    @Test
    void shouldAddSecret() {
        when(encryptionService.encrypt(plainSecretValue)).thenReturn(encryptedSecretValue);
        assertThat(web2SecretsService.addSecret(ownerAddress, secretAddress, plainSecretValue)).isTrue();
        verify(encryptionService).encrypt(any());
        verify(web2SecretsRepository).save(any());
    }

    @Test
    void shouldNotUpdateSecretIfMissing() {
        Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue);
        encryptedSecret.setEncryptedValue(true);
        Web2Secrets web2Secrets = new Web2Secrets(ownerAddress);
        web2Secrets.setSecrets(List.of());
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(web2Secrets));
        assertThatThrownBy(() -> web2SecretsService.updateSecret(ownerAddress, secretAddress, plainSecretValue))
                .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> web2SecretsService.updateSecret(ownerAddress, secretAddress, plainSecretValue))
                .isInstanceOf(NoSuchElementException.class);
        verify(web2SecretsRepository, never()).save(any());
    }

    @Test
    void shouldNotUpdateSecretIfSameValue() {
        Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue);
        encryptedSecret.setEncryptedValue(true);
        List<Secret> secretList = List.of(encryptedSecret);
        Web2Secrets web2Secrets = new Web2Secrets(ownerAddress);
        web2Secrets.setSecrets(secretList);
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress))
                .thenReturn(Optional.of(web2Secrets));
        when(encryptionService.encrypt(plainSecretValue))
                .thenReturn(encryptedSecretValue);

        web2SecretsService.updateSecret(ownerAddress, secretAddress, plainSecretValue);
        verify(web2SecretsRepository, never()).save(any());
    }

    @Test
    void shouldUpdateSecret() {
        Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue);
        encryptedSecret.setEncryptedValue(true);
        String newSecretValue = "newSecretValue";
        String newEncryptedSecretValue = "newEncryptedSecretValue";
        List<Secret> secretList = List.of(encryptedSecret);
        Web2Secrets web2SecretsMock = new Web2Secrets(ownerAddress);
        web2SecretsMock.setSecrets(secretList);
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress))
                .thenReturn(Optional.of(web2SecretsMock));
        when(encryptionService.encrypt(newSecretValue))
                .thenReturn(newEncryptedSecretValue);

        web2SecretsService.updateSecret(ownerAddress, secretAddress, newSecretValue);
        verify(web2SecretsRepository, times(1)).save(web2SecretsMock);
    }
}