/*
 * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
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

import ch.qos.logback.classic.Logger;
import com.iexec.sms.MemoryLogAppender;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Web2SecretServiceTests {
    private static final String OWNER_ADDRESS = "ownerAddress";
    private static final String SECRET_ADDRESS = "secretAddress";
    private static final String PLAIN_SECRET_VALUE = "plainSecretValue";
    private static final String ENCRYPTED_SECRET_VALUE = "encryptedSecretValue";

    @Autowired
    private Web2SecretRepository web2SecretRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private MeasuredSecretService measuredSecretService;

    private Web2SecretService web2SecretService;

    private static MemoryLogAppender memoryLogAppender;

    @BeforeAll
    void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.iexec.sms.secret");
        memoryLogAppender = (MemoryLogAppender) logger.getAppender("MEM");
    }

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        memoryLogAppender.reset();
        web2SecretRepository.deleteAll();
        web2SecretService = new Web2SecretService(web2SecretRepository, encryptionService, measuredSecretService);
    }


    // region getSecret
    @Test
    void shouldGetDecryptedValue() {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE);
        web2SecretRepository.save(encryptedSecret);

        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(PLAIN_SECRET_VALUE);

        final Optional<String> result = web2SecretService.getDecryptedValue(OWNER_ADDRESS, SECRET_ADDRESS);
        assertThat(result)
                .isNotEmpty()
                .get().isEqualTo(PLAIN_SECRET_VALUE);
    }

    @Test
    void shouldGetEncryptedSecret() {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE);
        web2SecretRepository.save(encryptedSecret);

        final Optional<Web2Secret> result = web2SecretService.getSecret(OWNER_ADDRESS, SECRET_ADDRESS);
        assertThat(result).isNotEmpty();
        assertAll(
                () -> assertThat(result).get().extracting(Web2Secret::getHeader).usingRecursiveComparison().isEqualTo(new Web2SecretHeader(OWNER_ADDRESS, SECRET_ADDRESS)),
                () -> assertThat(result).get().extracting(Web2Secret::getValue).isEqualTo(ENCRYPTED_SECRET_VALUE),
                () -> verifyNoInteractions(encryptionService)
        );
    }

    @Test
    void shouldGetEmptySecretIfSecretNotPresent() {
        assertThat(web2SecretService.getSecret(OWNER_ADDRESS, SECRET_ADDRESS)).isEmpty();
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldGetEmptyDecryptedValueIfSecretNotPresent() {
        assertThat(web2SecretService.getDecryptedValue(OWNER_ADDRESS, SECRET_ADDRESS)).isEmpty();
        verifyNoInteractions(encryptionService);
    }
    // endregion


    // region isSecretPresent
    @Test
    void shouldGetSecretExistFromDBAndPutInCache() {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE);
        web2SecretRepository.save(encryptedSecret);

        final boolean resultFirstCall = web2SecretService.isSecretPresent(OWNER_ADDRESS, SECRET_ADDRESS);

        assertAll(
                () -> assertTrue(resultFirstCall),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was not found in cache")),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
        );
    }

    @Test
    void shouldGetSecretExistFromCache() {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE);
        web2SecretRepository.save(encryptedSecret);

        web2SecretService.isSecretPresent(OWNER_ADDRESS, SECRET_ADDRESS);
        memoryLogAppender.reset();
        final boolean resultSecondCall = web2SecretService.isSecretPresent(OWNER_ADDRESS, SECRET_ADDRESS);
        assertAll(
                () -> assertTrue(resultSecondCall),
                () -> assertTrue(memoryLogAppender.doesNotContains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache")),
                () -> assertTrue(memoryLogAppender.contains("exist:true"))

        );
    }

    @Test
    void shouldGetSecretNotExistFromCache() {
        web2SecretService.isSecretPresent(OWNER_ADDRESS, SECRET_ADDRESS);
        memoryLogAppender.reset();
        boolean resultSecondCall = web2SecretService.isSecretPresent(OWNER_ADDRESS, SECRET_ADDRESS);
        assertAll(
                () -> assertFalse(resultSecondCall),
                () -> assertTrue(memoryLogAppender.doesNotContains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache")),
                () -> assertTrue(memoryLogAppender.contains("exist:false"))
        );
    }
    // endregion

    // region addSecret
    @Test
    void shouldAddSecret() throws SecretAlreadyExistsException {
        when(encryptionService.encrypt(PLAIN_SECRET_VALUE)).thenReturn(ENCRYPTED_SECRET_VALUE);

        final Web2Secret newSecret = web2SecretService.addSecret(OWNER_ADDRESS, SECRET_ADDRESS, PLAIN_SECRET_VALUE);
        assertAll(
                () -> assertThat(newSecret).extracting(Web2Secret::getHeader).usingRecursiveComparison().isEqualTo(new Web2SecretHeader(OWNER_ADDRESS, SECRET_ADDRESS)),
                () -> assertThat(newSecret).extracting(Web2Secret::getValue).isEqualTo(ENCRYPTED_SECRET_VALUE),
                () -> verify(measuredSecretService).newlyAddedSecret(),
                () -> verify(encryptionService).encrypt(any())
        );
    }

    @Test
    void shouldNotAddSecretIfPresent() {
        final Web2Secret secret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE);
        web2SecretRepository.save(secret);

        final SecretAlreadyExistsException exception = assertThrows(SecretAlreadyExistsException.class,
                () -> web2SecretService.addSecret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE));
        assertAll(
                () -> assertEquals(OWNER_ADDRESS, exception.getOwnerAddress()),
                () -> assertEquals(SECRET_ADDRESS, exception.getSecretAddress()),
                () -> verify(measuredSecretService, times(0)).newlyAddedSecret(),

                () -> verify(encryptionService, never()).encrypt(PLAIN_SECRET_VALUE),
                () -> assertThat(web2SecretRepository.count()).isOne()
        );
    }
    // endregion

    // region updateSecret
    @Test
    void shouldUpdateSecret() throws NotAnExistingSecretException, SameSecretException {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE);
        final String newSecretValue = "newSecretValue";
        final String newEncryptedSecretValue = "newEncryptedSecretValue";
        web2SecretRepository.save(encryptedSecret);
        when(encryptionService.encrypt(newSecretValue))
                .thenReturn(newEncryptedSecretValue);

        final Web2Secret newSecret = web2SecretService.updateSecret(OWNER_ADDRESS, SECRET_ADDRESS, newSecretValue);
        assertAll(
                () -> assertThat(newSecret).extracting(Web2Secret::getHeader).usingRecursiveComparison().isEqualTo(new Web2SecretHeader(OWNER_ADDRESS, SECRET_ADDRESS)),
                () -> assertThat(newSecret).extracting(Web2Secret::getValue).isEqualTo(newEncryptedSecretValue),
                () -> assertThat(web2SecretRepository.count()).isOne()    // A new object should be created with the same ID
        );
    }

    @Test
    void shouldNotUpdateSecretIfMissing() {
        final NotAnExistingSecretException exception = assertThrows(NotAnExistingSecretException.class,
                () -> web2SecretService.updateSecret(OWNER_ADDRESS, SECRET_ADDRESS, PLAIN_SECRET_VALUE));
        assertAll(
                () -> assertEquals(OWNER_ADDRESS, exception.getOwnerAddress()),
                () -> assertEquals(SECRET_ADDRESS, exception.getSecretAddress()),
                () -> assertThat(web2SecretRepository.count()).isZero()
        );
    }

    @Test
    void shouldNotUpdateSecretIfSameValue() {
        final Web2Secret encryptedSecret = new Web2Secret(OWNER_ADDRESS, SECRET_ADDRESS, ENCRYPTED_SECRET_VALUE);
        web2SecretRepository.save(encryptedSecret);
        when(encryptionService.encrypt(PLAIN_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);

        final SameSecretException exception = assertThrows(SameSecretException.class,
                () -> web2SecretService.updateSecret(OWNER_ADDRESS, SECRET_ADDRESS, PLAIN_SECRET_VALUE));
        assertAll(
                () -> assertEquals(OWNER_ADDRESS, exception.getOwnerAddress()),
                () -> assertEquals(SECRET_ADDRESS, exception.getSecretAddress()),
                () -> assertThat(web2SecretRepository.count()).isOne()
        );
    }
    // endregion
}
