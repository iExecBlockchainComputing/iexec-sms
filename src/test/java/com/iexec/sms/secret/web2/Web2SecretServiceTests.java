/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.secret.exception.NotAnExistingSecretException;
import com.iexec.sms.secret.exception.SameSecretException;
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
import static org.mockito.Mockito.never;

class Web2SecretServiceTests {
    private static final String OWNER_ADDRESS = "ownerAddress";
    private static final String SECRET_ADDRESS = "secretAddress";
    private static final Web2SecretHeader HEADER = new Web2SecretHeader(OWNER_ADDRESS, SECRET_ADDRESS);
    private static final String PLAIN_SECRET_VALUE = "plainSecretValue";
    private static final String ENCRYPTED_SECRET_VALUE = "encryptedSecretValue";

    @Mock
    private Web2SecretRepository web2SecretRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private MeasuredSecretService measuredSecretService;

    @InjectMocks
    private Web2SecretService web2SecretService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }


    // region updateSecret
    @Test
    void shouldUpdateSecret() throws NotAnExistingSecretException, SameSecretException {
        final Web2Secret encryptedSecret = new Web2Secret(HEADER, ENCRYPTED_SECRET_VALUE);
        final String newSecretValue = "newSecretValue";
        final String newEncryptedSecretValue = "newEncryptedSecretValue";
        when(web2SecretRepository.findById(HEADER))
                .thenReturn(Optional.of(encryptedSecret));
        when(encryptionService.encrypt(newSecretValue))
                .thenReturn(newEncryptedSecretValue);
        final Web2Secret savedSecret = new Web2Secret(HEADER, newEncryptedSecretValue);
        when(web2SecretRepository.save(any()))
                .thenReturn(savedSecret);

        final Web2Secret newSecret = web2SecretService.updateSecret(HEADER, newSecretValue);
        assertAll(
                () -> assertThat(newSecret).extracting(Web2Secret::getHeader).usingRecursiveComparison().isEqualTo(HEADER),
                () -> assertThat(newSecret).extracting(Web2Secret::getValue).isEqualTo(newEncryptedSecretValue),

                () -> verify(web2SecretRepository, never()).save(encryptedSecret),   // Current object should not be updated
                () -> verify(web2SecretRepository, times(1)).save(any())    // A new object should be created with the same ID
        );
    }

    @Test
    void shouldNotUpdateSecretIfMissing() {
        when(web2SecretRepository.findById(HEADER))
                .thenReturn(Optional.empty());

        final NotAnExistingSecretException exception = assertThrows(NotAnExistingSecretException.class,
                () -> web2SecretService.updateSecret(HEADER, PLAIN_SECRET_VALUE));
        assertAll(
                () -> assertEquals(HEADER, exception.getHeader()),
                () -> verify(web2SecretRepository, never()).save(any())
        );
    }

    @Test
    void shouldNotUpdateSecretIfSameValue() {
        final Web2Secret encryptedSecret = new Web2Secret(HEADER, ENCRYPTED_SECRET_VALUE);
        when(web2SecretRepository.findById(HEADER))
                .thenReturn(Optional.of(encryptedSecret));
        when(encryptionService.encrypt(PLAIN_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);

        final SameSecretException exception = assertThrows(SameSecretException.class,
                () -> web2SecretService.updateSecret(HEADER, PLAIN_SECRET_VALUE));
        assertAll(
                () -> assertEquals(HEADER, exception.getHeader()),
                () -> verify(web2SecretRepository, never()).save(any())
        );
    }
    // endregion
}
