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

package com.iexec.sms.secret.base;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.secret.exception.SecretAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static com.iexec.sms.secret.base.TestSecret.*;
import static com.iexec.sms.secret.base.TestSecretHeader.*;
import static com.iexec.sms.secret.base.TestSecretHeader.TEST_SECRET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

class AbstractSecretServiceTests {
    @Mock
    private TestSecretRepository testSecretRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private MeasuredSecretService measuredSecretService;

    @InjectMocks
    private TestSecretService testSecretService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }


    // region getSecret
    @Test
    void shouldGetDecryptedValue() {
        when(testSecretRepository.findById(TEST_SECRET_HEADER))
                .thenReturn(Optional.of(TEST_SECRET));
        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(PLAIN_SECRET_VALUE);

        final Optional<String> result = testSecretService.getDecryptedValue(TEST_SECRET_HEADER);
        assertThat(result)
                .isNotEmpty()
                .get()
                .isEqualTo(PLAIN_SECRET_VALUE);
    }

    @Test
    void shouldGetEncryptedSecret() {
        final TestSecret encryptedSecret = new TestSecret(TEST_SECRET_HEADER, ENCRYPTED_SECRET_VALUE);

        when(testSecretRepository.findById(TEST_SECRET_HEADER))
                .thenReturn(Optional.of(encryptedSecret));

        final Optional<TestSecret> result = testSecretService.getSecret(TEST_SECRET_HEADER);
        assertThat(result).isNotEmpty();
        assertAll(
                () -> assertThat(result).get().extracting(TestSecret::getHeader).usingRecursiveComparison().isEqualTo(TEST_SECRET_HEADER),
                () -> assertThat(result).get().extracting(TestSecret::getValue).isEqualTo(ENCRYPTED_SECRET_VALUE),
                () -> verifyNoInteractions(encryptionService)
        );
    }

    @Test
    void shouldGetEmptySecretIfSecretNotPresent() {
        when(testSecretRepository.findById(TEST_SECRET_HEADER))
                .thenReturn(Optional.empty());

        assertThat(testSecretService.getSecret(TEST_SECRET_HEADER)).isEmpty();
        verify(testSecretRepository, times(1))
                .findById(TEST_SECRET_HEADER);
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldGetEmptyDecryptedValueIfSecretNotPresent() {
        when(testSecretRepository.findById(TEST_SECRET_HEADER))
                .thenReturn(Optional.empty());

        assertThat(testSecretService.getDecryptedValue(TEST_SECRET_HEADER)).isEmpty();
        verify(testSecretRepository, times(1))
                .findById(TEST_SECRET_HEADER);
        verifyNoInteractions(encryptionService);
    }
    // endregion

    // region addSecret
    @Test
    void shouldAddSecret() {
        when(testSecretRepository.findById(TEST_SECRET_HEADER))
                .thenReturn(Optional.empty());
        when(encryptionService.encrypt(PLAIN_SECRET_VALUE)).thenReturn(ENCRYPTED_SECRET_VALUE);
        when(testSecretRepository.save(any())).thenReturn(new TestSecret(TEST_SECRET_HEADER, ENCRYPTED_SECRET_VALUE));

        final TestSecret result = assertDoesNotThrow(() -> testSecretService.addSecret(TEST_SECRET_HEADER, PLAIN_SECRET_VALUE));
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> verify(measuredSecretService).newlyAddedSecret(),

                () -> verify(encryptionService).encrypt(any()),
                () -> verify(testSecretRepository).save(any())
        );
    }

    @Test
    void shouldNotAddSecretIfPresent() {
        final TestSecret secret = new TestSecret(TEST_SECRET_HEADER, ENCRYPTED_SECRET_VALUE);
        when(testSecretRepository.findById(TEST_SECRET_HEADER))
                .thenReturn(Optional.of(secret));

        final SecretAlreadyExistsException exception = assertThrows(SecretAlreadyExistsException.class,
                () -> testSecretService.addSecret(TEST_SECRET_HEADER, ENCRYPTED_SECRET_VALUE));
        assertAll(
                () -> assertEquals(TEST_SECRET_ID, ((TestSecretHeader)exception.getHeader()).getId()),
                () -> verify(measuredSecretService, times(0)).newlyAddedSecret(),

                () -> verify(encryptionService, never()).encrypt(PLAIN_SECRET_VALUE),
                () -> verify(testSecretRepository, never()).save(any())
        );
    }
    // endregion
}