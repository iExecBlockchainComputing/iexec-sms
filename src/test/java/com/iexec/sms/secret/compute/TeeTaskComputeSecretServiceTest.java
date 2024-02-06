/*
 * Copyright 2021-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.secret.compute;

import ch.qos.logback.classic.Logger;
import com.iexec.sms.MemoryLogAppender;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.CacheSecretService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeeTaskComputeSecretServiceTest {
    private static final String APP_ADDRESS = "appAddress";
    private static final String DECRYPTED_SECRET_VALUE = "I'm a secret.";
    private static final String ENCRYPTED_SECRET_VALUE = "I'm an encrypted secret.";
    private static final TeeTaskComputeSecret COMPUTE_SECRET = TeeTaskComputeSecret
            .builder()
            .onChainObjectType(OnChainObjectType.APPLICATION)
            .onChainObjectAddress(APP_ADDRESS.toLowerCase())
            .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
            .fixedSecretOwner("")
            .key("0")
            .value(ENCRYPTED_SECRET_VALUE)
            .build();

    @Autowired
    TeeTaskComputeSecretRepository teeTaskComputeSecretRepository;

    @Mock
    EncryptionService encryptionService;

    @Mock
    MeasuredSecretService measuredSecretService;

    CacheSecretService<TeeTaskComputeSecretHeader> teeTaskComputeCacheSecretService;

    TeeTaskComputeSecretService teeTaskComputeSecretService;

    private MemoryLogAppender memoryLogAppender;

    @BeforeAll
    void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.iexec.sms.secret");
        memoryLogAppender = (MemoryLogAppender) logger.getAppender("MEM");
        teeTaskComputeCacheSecretService = new CacheSecretService<>();
    }

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        memoryLogAppender.reset();
        teeTaskComputeSecretRepository.deleteAll();
        teeTaskComputeCacheSecretService.clear();
        teeTaskComputeSecretService = new TeeTaskComputeSecretService(
                teeTaskComputeSecretRepository, encryptionService, measuredSecretService, teeTaskComputeCacheSecretService);
    }

    // region encryptAndSaveSecret
    @Test
    void shouldAddSecret() {
        when(encryptionService.encrypt(DECRYPTED_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);
        final boolean secretAdded = teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0", DECRYPTED_SECRET_VALUE);

        assertAll(
                () -> assertThat(secretAdded).isTrue(),
                () -> assertThat(teeTaskComputeSecretRepository.count()).isOne(),
                () -> assertTrue(memoryLogAppender.contains("Adding new tee task compute secret")),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
        );

        final TeeTaskComputeSecret savedTeeTaskComputeSecret = teeTaskComputeSecretRepository
                .findById(new TeeTaskComputeSecretHeader(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0"))
                .orElseThrow();
        assertAll(
                () -> assertEquals("0", savedTeeTaskComputeSecret.getHeader().getKey()),
                () -> assertEquals(savedTeeTaskComputeSecret.getHeader().getOnChainObjectAddress(), APP_ADDRESS.toLowerCase()),
                () -> assertEquals(ENCRYPTED_SECRET_VALUE, savedTeeTaskComputeSecret.getValue()),
                () -> verify(measuredSecretService).newlyAddedSecret()
        );
    }

    @Test
    void shouldNotAddSecretSinceAlreadyExist() {
        teeTaskComputeSecretRepository.save(COMPUTE_SECRET);

        teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0", DECRYPTED_SECRET_VALUE);

        assertThat(teeTaskComputeSecretRepository.count()).isOne();
        verify(measuredSecretService, times(0)).newlyAddedSecret();
    }
    // endregion

    // region getSecret
    @Test
    void shouldGetSecret() {
        teeTaskComputeSecretRepository.save(COMPUTE_SECRET);
        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(DECRYPTED_SECRET_VALUE);

        Optional<TeeTaskComputeSecret> decryptedSecret = teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        Assertions.assertThat(decryptedSecret).isPresent();
        Assertions.assertThat(decryptedSecret.get().getHeader().getKey()).isEqualTo("0");
        Assertions.assertThat(decryptedSecret.get().getHeader().getOnChainObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(decryptedSecret.get().getValue()).isEqualTo(DECRYPTED_SECRET_VALUE);
        verify(encryptionService, Mockito.times(1)).decrypt(any());
    }
    // endregion

    // region isSecretPresent
    @Test
    void shouldGetSecretExistFromDBAndPutInCache() {
        teeTaskComputeSecretRepository.save(COMPUTE_SECRET);

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertTrue(isSecretPresent),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was not found in cache")),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
        );
    }

    @Test
    void shouldGetSecretExistFromCache() {
        teeTaskComputeSecretRepository.save(COMPUTE_SECRET);

        teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        memoryLogAppender.reset();
        final boolean resultSecondCall = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

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
        teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        memoryLogAppender.reset();
        final boolean resultSecondCall = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertFalse(resultSecondCall),
                () -> assertTrue(memoryLogAppender.doesNotContains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache")),
                () -> assertTrue(memoryLogAppender.contains("exist:false"))
        );
    }

    @Test
    void secretShouldNotExist() {
        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        assertThat(isSecretPresent).isFalse();
    }
    // endregion
}
