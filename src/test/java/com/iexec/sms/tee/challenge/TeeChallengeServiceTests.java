/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.challenge;

import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.sms.chain.IexecHubService;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import com.iexec.sms.tee.config.TeeChallengeCleanupConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DataJpaTest
@ExtendWith(MockitoExtension.class)
class TeeChallengeServiceTests {

    private static final String TASK_ID = "0x123";
    private static final String PLAIN_PRIVATE = "plainPrivate";
    private static final String ENC_PRIVATE = "encPrivate";

    private final Instant finalDeadline = Instant.now().minusMillis(1000);

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EthereumCredentialsRepository ethereumCredentialsRepository;
    @Autowired
    private TeeChallengeRepository teeChallengeRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private IexecHubService iexecHubService;

    @Mock
    private MeasuredSecretService teeChallengeMeasuredSecretService;

    @Mock
    private MeasuredSecretService ethereumCredentialsMeasuredSecretService;

    private TeeChallengeService teeChallengeService;

    @BeforeEach
    void beforeEach() {
        teeChallengeRepository.deleteAll();
        final TeeChallengeCleanupConfiguration cleanupConfiguration = new TeeChallengeCleanupConfiguration(
                "@hourly", 1, Duration.ofMinutes(1));
        teeChallengeService = new TeeChallengeService(
                jdbcTemplate,
                teeChallengeRepository,
                encryptionService,
                iexecHubService,
                teeChallengeMeasuredSecretService,
                ethereumCredentialsMeasuredSecretService,
                cleanupConfiguration
        );
    }

    private TeeChallenge getEncryptedTeeChallengeStub() throws GeneralSecurityException {
        TeeChallenge teeChallenge = new TeeChallenge(TASK_ID, finalDeadline);
        teeChallenge.getCredentials().setEncryptedPrivateKey(ENC_PRIVATE);
        return teeChallenge;
    }

    // region getOrCreate
    @Test
    void shouldGetExistingChallengeWithoutDecryptingKeys() throws GeneralSecurityException {
        TeeChallenge encryptedTeeChallengeStub = getEncryptedTeeChallengeStub();
        teeChallengeRepository.save(encryptedTeeChallengeStub);

        Optional<TeeChallenge> oTeeChallenge = teeChallengeService.getOrCreate(TASK_ID, false);

        assertThat(oTeeChallenge).isPresent();
        assertThat(oTeeChallenge.get().getCredentials().getPrivateKey()).isEqualTo(ENC_PRIVATE);
        assertThat(teeChallengeRepository.count()).isOne();
        assertThat(ethereumCredentialsRepository.count()).isOne();
        verify(encryptionService, never()).decrypt(anyString());
        verifyNoInteractions(teeChallengeMeasuredSecretService, ethereumCredentialsMeasuredSecretService);
    }

    @Test
    void shouldGetExistingChallengeAndDecryptKeys() throws GeneralSecurityException {
        TeeChallenge encryptedTeeChallengeStub = getEncryptedTeeChallengeStub();
        teeChallengeRepository.save(encryptedTeeChallengeStub);
        when(encryptionService.decrypt(anyString())).thenReturn(PLAIN_PRIVATE);

        Optional<TeeChallenge> oTeeChallenge = teeChallengeService.getOrCreate(TASK_ID, true);

        assertThat(oTeeChallenge).isPresent();
        assertThat(oTeeChallenge.get().getCredentials().getPrivateKey()).isEqualTo(PLAIN_PRIVATE);
        assertThat(teeChallengeRepository.count()).isOne();
        assertThat(ethereumCredentialsRepository.count()).isOne();
        verify(encryptionService).decrypt(anyString());
        verifyNoInteractions(teeChallengeMeasuredSecretService, ethereumCredentialsMeasuredSecretService);
    }

    @Test
    void shouldCreateNewChallengeWithoutDecryptingKeys() {
        when(iexecHubService.getTaskDescription(TASK_ID))
                .thenReturn(TaskDescription.builder().finalDeadline(finalDeadline.toEpochMilli()).build());
        when(encryptionService.encrypt(anyString())).thenReturn(ENC_PRIVATE);

        Optional<TeeChallenge> oTeeChallenge = teeChallengeService.getOrCreate(TASK_ID, false);

        assertThat(oTeeChallenge).isPresent();
        assertThat(oTeeChallenge.get().getCredentials().getPrivateKey()).isEqualTo(ENC_PRIVATE);
        assertThat(teeChallengeRepository.count()).isOne();
        assertThat(ethereumCredentialsRepository.count()).isOne();
        verify(encryptionService, never()).decrypt(anyString());
        verify(teeChallengeMeasuredSecretService).newlyAddedSecret();
        verify(ethereumCredentialsMeasuredSecretService).newlyAddedSecret();
    }

    @Test
    void shouldCreateNewChallengeAndDecryptKeys() {
        when(iexecHubService.getTaskDescription(TASK_ID))
                .thenReturn(TaskDescription.builder().finalDeadline(finalDeadline.toEpochMilli()).build());
        when(encryptionService.encrypt(anyString())).thenReturn(ENC_PRIVATE);
        when(encryptionService.decrypt(anyString())).thenReturn(PLAIN_PRIVATE);

        Optional<TeeChallenge> oTeeChallenge = teeChallengeService.getOrCreate(TASK_ID, true);

        assertThat(oTeeChallenge).isPresent();
        assertThat(oTeeChallenge.get().getCredentials().getPrivateKey()).isEqualTo(PLAIN_PRIVATE);
        assertThat(teeChallengeRepository.count()).isOne();
        assertThat(ethereumCredentialsRepository.count()).isOne();
        verify(teeChallengeMeasuredSecretService).newlyAddedSecret();
        verify(ethereumCredentialsMeasuredSecretService).newlyAddedSecret();
    }
    // endregion

    // region encryptChallengeKeys
    @Test
    void shouldEncryptChallengeKeys() throws GeneralSecurityException {
        TeeChallenge teeChallenge = new TeeChallenge(TASK_ID, finalDeadline);
        when(encryptionService.encrypt(anyString())).thenReturn(ENC_PRIVATE);
        teeChallengeService.encryptChallengeKeys(teeChallenge);

        assertThat(teeChallenge.getCredentials().getPrivateKey()).isEqualTo(ENC_PRIVATE);
    }
    // endregion

    // region decryptChallengeKeys
    @Test
    void shouldDecryptChallengeKeys() {
        TeeChallenge teeChallenge = TeeChallenge.builder()
                .taskId(TASK_ID)
                .credentials(new EthereumCredentials("id", "pk", true, "address"))
                .build();
        when(encryptionService.decrypt(anyString())).thenReturn(PLAIN_PRIVATE);

        teeChallengeService.decryptChallengeKeys(teeChallenge);
        assertThat(teeChallenge.getCredentials().getPrivateKey()).isEqualTo(PLAIN_PRIVATE);
    }
    // endregion

    // region cleanExpiredChallenge
    @Test
    void shouldPurgeExpiredChallenge() throws GeneralSecurityException {
        final TeeChallenge encryptedTeeChallengeStub = getEncryptedTeeChallengeStub();
        teeChallengeRepository.save(encryptedTeeChallengeStub);

        teeChallengeService.cleanExpiredTasksTeeChallenges();

        assertThat(teeChallengeRepository.count()).isZero();
        assertThat(ethereumCredentialsRepository.count()).isZero();
    }

    @Test
    void shouldSetChallengeFinalDeadlineWhenUnset() throws GeneralSecurityException {
        final TeeChallenge teeChallenge = new TeeChallenge(TASK_ID, null);
        teeChallenge.getCredentials().setEncryptedPrivateKey(ENC_PRIVATE);
        final TeeChallenge savedChallenge = teeChallengeRepository.save(teeChallenge);

        assertThat(teeChallengeRepository.countByFinalDeadlineIsNull()).isOne();

        teeChallengeService.cleanExpiredTasksTeeChallenges();
        // refresh entity to update cache with new database state
        entityManager.refresh(savedChallenge);

        assertThat(teeChallengeRepository.count()).isOne();
        assertThat(ethereumCredentialsRepository.count()).isOne();

        assertThat(teeChallengeRepository.countByFinalDeadlineIsNull()).isZero();
        final TeeChallenge currentChallenge = teeChallengeRepository.findByTaskId(TASK_ID).orElseThrow();
        assertThat(currentChallenge).isNotNull();
        assertThat(currentChallenge.getFinalDeadline())
                .isNotNull()
                .isAfter(Instant.now());
    }
    // endregion
}
