/*
 *
 *  * Copyright 2022 IEXEC BLOCKCHAIN TECH
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.iexec.sms.secret.web2;

import com.iexec.sms.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Web2SecretServiceTests {
    private static final String OWNER_ADDRESS = "ownerAddress";
    private static final String SECRET_ADDRESS = "secretAddress";
    private static final String PLAIN_SECRET_VALUE = "plainSecretValue";
    private static final String ENCRYPTED_SECRET_VALUE = "encryptedSecretValue";

    @Mock
    private Web2SecretRepository web2SecretRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private Web2SecretService web2SecretService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region getSecret
    @Test
    void shouldGetDecryptedSecret() {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE, true);

        when(web2SecretRepository.findById(any(Web2SecretHeader.class)))
                .thenReturn(Optional.of(encryptedSecret));
        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(PLAIN_SECRET_VALUE);

        final Optional<Web2Secret> result = web2SecretService.getSecret(OWNER_ADDRESS, SECRET_ADDRESS, true);
        assertThat(result).isNotEmpty();
        assertAll(
                () -> assertThat(result).get().extracting(Web2Secret::getHeader).usingRecursiveComparison().isEqualTo(new Web2SecretHeader(OWNER_ADDRESS, SECRET_ADDRESS)),
                () -> assertThat(result).get().extracting(Web2Secret::getValue).isEqualTo(PLAIN_SECRET_VALUE),
                () -> assertThat(result).get().extracting(Web2Secret::isEncryptedValue).isEqualTo(false)
        );
    }

    @Test
    void shouldGetEncryptedSecret() {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE, true);

        when(web2SecretRepository.findById(any(Web2SecretHeader.class)))
                .thenReturn(Optional.of(encryptedSecret));

        final Optional<Web2Secret> oSecret1 = web2SecretService.getSecret(OWNER_ADDRESS, SECRET_ADDRESS);
        final Optional<Web2Secret> oSecret2 = web2SecretService.getSecret(OWNER_ADDRESS, SECRET_ADDRESS, false);
        assertThat(oSecret1).isNotEmpty();
        assertAll(
                () -> assertThat(oSecret1).isEqualTo(oSecret2),
                () -> assertThat(oSecret1).get().extracting(Web2Secret::getHeader).usingRecursiveComparison().isEqualTo(new Web2SecretHeader(OWNER_ADDRESS, SECRET_ADDRESS)),
                () -> assertThat(oSecret1).get().extracting(Web2Secret::getValue).isEqualTo(ENCRYPTED_SECRET_VALUE),
                () -> assertThat(oSecret1).get().extracting(Web2Secret::isEncryptedValue).isEqualTo(true),
                () -> verifyNoInteractions(encryptionService)
        );
    }

    @Test
    void shouldGetEmptyResultIfSecretNotPresent() {
        when(web2SecretRepository.findById(any(Web2SecretHeader.class)))
                .thenReturn(Optional.empty());

        assertThat(web2SecretService.getSecret(OWNER_ADDRESS, SECRET_ADDRESS)).isEmpty();
        assertThat(web2SecretService.getSecret(OWNER_ADDRESS, SECRET_ADDRESS, false)).isEmpty();
        verify(web2SecretRepository, times(2))
                .findById(any(Web2SecretHeader.class));
        verifyNoInteractions(encryptionService);
    }
    // endregion

    // region addSecret
    @Test
    void shouldAddSecret() throws SecretAlreadyExistsException {
        when(web2SecretRepository.findById(any(Web2SecretHeader.class)))
                .thenReturn(Optional.empty());
        when(encryptionService.encrypt(PLAIN_SECRET_VALUE)).thenReturn(ENCRYPTED_SECRET_VALUE);
        when(web2SecretRepository.save(any())).thenReturn(new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE, true));

        final Web2Secret newSecret = web2SecretService.addSecret(OWNER_ADDRESS, SECRET_ADDRESS, PLAIN_SECRET_VALUE);
        assertAll(
                () -> assertThat(newSecret).extracting(Web2Secret::getHeader).usingRecursiveComparison().isEqualTo(new Web2SecretHeader(OWNER_ADDRESS, SECRET_ADDRESS)),
                () -> assertThat(newSecret).extracting(Web2Secret::getValue).isEqualTo(ENCRYPTED_SECRET_VALUE),
                () -> assertThat(newSecret).extracting(Web2Secret::isEncryptedValue).isEqualTo(true),

                () -> verify(encryptionService).encrypt(any()),
                () -> verify(web2SecretRepository).save(any())
        );
    }

    @Test
    void shouldNotAddSecretIfPresent() {
        final Web2Secret secret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE, true);
        when(web2SecretRepository.findById(any(Web2SecretHeader.class)))
                .thenReturn(Optional.of(secret));

        final SecretAlreadyExistsException exception = assertThrows(SecretAlreadyExistsException.class,
                () -> web2SecretService.addSecret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE));
        assertAll(
                () -> assertEquals(OWNER_ADDRESS, exception.getOwnerAddress()),
                () -> assertEquals(SECRET_ADDRESS, exception.getSecretAddress()),
                () -> verify(encryptionService, never()).encrypt(PLAIN_SECRET_VALUE),
                () -> verify(web2SecretRepository, never()).save(any())
        );
    }
    // endregion

    // region updateSecret
    @Test
    void shouldUpdateSecret() throws NotAnExistingSecretException, SameSecretException {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE, true);
        final String newSecretValue = "newSecretValue";
        final String newEncryptedSecretValue = "newEncryptedSecretValue";
        when(web2SecretRepository.findById(any(Web2SecretHeader.class)))
                .thenReturn(Optional.of(encryptedSecret));
        when(encryptionService.encrypt(newSecretValue))
                .thenReturn(newEncryptedSecretValue);
        final Web2Secret savedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, newEncryptedSecretValue, true);
        when(web2SecretRepository.save(any()))
                .thenReturn(savedSecret);

        final Web2Secret newSecret = web2SecretService.updateSecret(OWNER_ADDRESS, SECRET_ADDRESS, newSecretValue);
        assertAll(
                () -> assertThat(newSecret).extracting(Web2Secret::getHeader).usingRecursiveComparison().isEqualTo(new Web2SecretHeader(OWNER_ADDRESS, SECRET_ADDRESS)),
                () -> assertThat(newSecret).extracting(Web2Secret::getValue).isEqualTo(newEncryptedSecretValue),
                () -> assertThat(newSecret).extracting(Web2Secret::isEncryptedValue).isEqualTo(true),

                () -> verify(web2SecretRepository, never()).save(encryptedSecret),   // Current object should not be updated
                () -> verify(web2SecretRepository, times(1)).save(any())    // A new object should be created with the same ID
        );
    }

    @Test
    void shouldNotUpdateSecretIfMissing() {
        when(web2SecretRepository.findById(any(Web2SecretHeader.class)))
                .thenReturn(Optional.empty());

        final NotAnExistingSecretException exception = assertThrows(NotAnExistingSecretException.class,
                () -> web2SecretService.updateSecret(OWNER_ADDRESS, SECRET_ADDRESS, PLAIN_SECRET_VALUE));
        assertAll(
                () -> assertEquals(OWNER_ADDRESS, exception.getOwnerAddress()),
                () -> assertEquals(SECRET_ADDRESS, exception.getSecretAddress()),
                () -> verify(web2SecretRepository, never()).save(any())
        );
    }

    @Test
    void shouldNotUpdateSecretIfSameValue() {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE, true);
        when(web2SecretRepository.findById(any(Web2SecretHeader.class)))
                .thenReturn(Optional.of(encryptedSecret));
        when(encryptionService.encrypt(PLAIN_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);

        final SameSecretException exception = assertThrows(SameSecretException.class,
                () -> web2SecretService.updateSecret(OWNER_ADDRESS, SECRET_ADDRESS, PLAIN_SECRET_VALUE));
        assertAll(
                () -> assertEquals(OWNER_ADDRESS, exception.getOwnerAddress()),
                () -> assertEquals(SECRET_ADDRESS, exception.getSecretAddress()),
                () -> verify(web2SecretRepository, never()).save(any())
        );
    }
    // endregion
}
