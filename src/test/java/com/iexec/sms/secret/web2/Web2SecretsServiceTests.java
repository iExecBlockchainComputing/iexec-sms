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
import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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
    @Spy
    private Web2SecretsService web2SecretsService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldGetDecryptedSecret() {
        Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue, true);
        Web2Secrets web2SecretsMock = new Web2Secrets(ownerAddress, List.of(encryptedSecret));
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress))
                .thenReturn(Optional.of(web2SecretsMock));
        when(encryptionService.decrypt(encryptedSecret.getValue()))
                .thenReturn(plainSecretValue);

        Optional<Secret> result = web2SecretsService.getSecret(ownerAddress, secretAddress, true);
        assertThat(result).isNotEmpty();
        assertThat(result).get().extracting(Secret::getAddress).isEqualTo(secretAddress);
        assertThat(result).get().extracting(Secret::getValue).isEqualTo(plainSecretValue);
        assertThat(result).get().extracting(Secret::isEncryptedValue).isEqualTo(false);
    }

    @Test
    void shouldGetEncryptedSecret() {
        Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue, true);
        Web2Secrets web2Secrets = new Web2Secrets(ownerAddress, List.of(encryptedSecret));
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
        Web2Secrets web2Secrets = new Web2Secrets(ownerAddress, List.of(new Secret(secretAddress, encryptedSecretValue, true)));
        when(web2SecretsRepository.findWeb2SecretsByOwnerAddress(ownerAddress)).thenReturn(Optional.of(web2Secrets));
        assertThat(web2SecretsService.addSecret(ownerAddress, secretAddress, plainSecretValue)).isEmpty();
        verify(encryptionService, times(1)).encrypt(plainSecretValue);
        verify(web2SecretsRepository, never()).save(any());
    }

    @Test
    void shouldAddSecret() {
        when(encryptionService.encrypt(plainSecretValue)).thenReturn(encryptedSecretValue);

        final Optional<Secret> newSecret = web2SecretsService.addSecret(ownerAddress, secretAddress, plainSecretValue);
        assertThat(newSecret).isPresent();
        assertThat(newSecret).get().extracting(Secret::getAddress).isEqualTo(secretAddress);
        assertThat(newSecret).get().extracting(Secret::getValue).isEqualTo(encryptedSecretValue);
        assertThat(newSecret).get().extracting(Secret::isEncryptedValue).isEqualTo(true);

        verify(encryptionService).encrypt(any());
        verify(web2SecretsRepository).save(any());
    }

    @Test
    void shouldNotUpdateSecretIfMissing() {
        final Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue, true);
        final Web2Secrets web2Secrets = new Web2Secrets(ownerAddress, List.of(encryptedSecret));
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
    void shouldNotUpdateSecretIfSameValue() throws NotAnExistingSecretException {
        final Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue, true);
        final Web2Secrets web2Secrets = new Web2Secrets(ownerAddress, List.of(encryptedSecret));
        when(web2SecretsService.getWeb2Secrets(ownerAddress))
                .thenReturn(Optional.of(web2Secrets));
        when(encryptionService.encrypt(plainSecretValue))
                .thenReturn(encryptedSecretValue);

        assertThat(web2SecretsService.updateSecret(ownerAddress, secretAddress, plainSecretValue)).isEmpty();
        verify(web2SecretsRepository, never()).save(web2Secrets);
    }

    @Test
    void shouldNotUpdateSecretIfOldSecretDoesntExist() {
        final Web2Secrets web2Secrets = new Web2Secrets(ownerAddress, List.of());
        when(web2SecretsService.getWeb2Secrets(ownerAddress))
                .thenReturn(Optional.of(web2Secrets));
        when(encryptionService.encrypt(plainSecretValue))
                .thenReturn(encryptedSecretValue);

        final ThrowableAssertAlternative<NotAnExistingSecretException> exception =
                assertThatExceptionOfType(NotAnExistingSecretException.class)
                .isThrownBy(() -> web2SecretsService.updateSecret(ownerAddress, secretAddress, plainSecretValue));
        exception.extracting(NotAnExistingSecretException::getOwnerAddress).isEqualTo(ownerAddress);
        exception.extracting(NotAnExistingSecretException::getSecretAddress).isEqualTo(secretAddress);

        verify(web2SecretsRepository, never()).save(any());
    }

    @Test
    void shouldUpdateSecret() throws NotAnExistingSecretException {
        Secret encryptedSecret = new Secret(secretAddress, encryptedSecretValue, true);
        String newSecretValue = "newSecretValue";
        String newEncryptedSecretValue = "newEncryptedSecretValue";
        Web2Secrets web2SecretsMock = new Web2Secrets(ownerAddress, List.of(encryptedSecret));
        when(web2SecretsService.getWeb2Secrets(ownerAddress))
                .thenReturn(Optional.of(web2SecretsMock));
        when(encryptionService.encrypt(newSecretValue))
                .thenReturn(newEncryptedSecretValue);

        final Optional<Secret> oNewSecret = web2SecretsService.updateSecret(ownerAddress, secretAddress, newSecretValue);
        assertThat(oNewSecret).isPresent();

        final Secret newSecret = oNewSecret.get();
        assertThat(newSecret).extracting(Secret::getAddress).isEqualTo(secretAddress);
        assertThat(newSecret).extracting(Secret::getValue).isEqualTo(newEncryptedSecretValue);
        assertThat(newSecret).extracting(Secret::isEncryptedValue).isEqualTo(true);

        verify(web2SecretsRepository, never()).save(web2SecretsMock);   // Current object should not be updated
        verify(web2SecretsRepository, times(1)).save(any());    // A new object should be created with the same ID
    }
}