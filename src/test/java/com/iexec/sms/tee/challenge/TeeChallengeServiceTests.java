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

package com.iexec.sms.tee.challenge;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TeeChallengeServiceTests {

    private static final String TASK_ID = "0x123";
    private static final String PLAIN_PRIVATE = "plainPrivate";
    private static final String ENC_PRIVATE = "encPrivate";

    @Mock
    private TeeChallengeRepository teeChallengeRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private MeasuredSecretService teeChallengeMeasuredSecretService;

    @Mock
    private MeasuredSecretService ethereumCredentialsMeasuredSecretService;

    private TeeChallengeService teeChallengeService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        teeChallengeService = new TeeChallengeService(teeChallengeRepository,
                encryptionService,
                teeChallengeMeasuredSecretService,
                ethereumCredentialsMeasuredSecretService
        );
    }

    private TeeChallenge getEncryptedTeeChallengeStub() throws Exception {
        TeeChallenge teeChallenge = new TeeChallenge(TASK_ID);
        teeChallenge.getCredentials().setEncryptedPrivateKey(ENC_PRIVATE);
        return teeChallenge;
    }

    @Test
    void shouldGetExistingChallengeWithoutDecryptingKeys() throws Exception {
        TeeChallenge encryptedTeeChallengeStub = getEncryptedTeeChallengeStub();
        when(teeChallengeRepository.findByTaskId(TASK_ID)).thenReturn(Optional.of(encryptedTeeChallengeStub));

        Optional<TeeChallenge> oTeeChallenge = teeChallengeService.getOrCreate(TASK_ID, false);
        assertThat(oTeeChallenge).isPresent();
        assertThat(oTeeChallenge.get().getCredentials().getPrivateKey()).isEqualTo(ENC_PRIVATE);
        verify(encryptionService, never()).decrypt(anyString());

        verify(teeChallengeMeasuredSecretService, never()).newlyAddedSecret();
        verify(ethereumCredentialsMeasuredSecretService, never()).newlyAddedSecret();
    }

    @Test
    void shouldGetExistingChallengeAndDecryptKeys() throws Exception {
        TeeChallenge encryptedTeeChallengeStub = getEncryptedTeeChallengeStub();
        when(teeChallengeRepository.findByTaskId(TASK_ID)).thenReturn(Optional.of(encryptedTeeChallengeStub));
        when(encryptionService.decrypt(anyString())).thenReturn(PLAIN_PRIVATE);

        Optional<TeeChallenge> oTeeChallenge = teeChallengeService.getOrCreate(TASK_ID, true);
        assertThat(oTeeChallenge).isPresent();
        assertThat(oTeeChallenge.get().getCredentials().getPrivateKey()).isEqualTo(PLAIN_PRIVATE);
        verify(encryptionService, times(1)).decrypt(anyString());

        verify(teeChallengeMeasuredSecretService, never()).newlyAddedSecret();
        verify(ethereumCredentialsMeasuredSecretService, never()).newlyAddedSecret();
    }

    @Test
    void shouldCreateNewChallengeWithoutDecryptingKeys() throws Exception {
        TeeChallenge encryptedTeeChallengeStub = getEncryptedTeeChallengeStub();
        when(teeChallengeRepository.findByTaskId(TASK_ID)).thenReturn(Optional.empty());
        when(encryptionService.encrypt(anyString())).thenReturn(ENC_PRIVATE);
        when(teeChallengeRepository.save(any())).thenReturn(encryptedTeeChallengeStub);

        Optional<TeeChallenge> oTeeChallenge = teeChallengeService.getOrCreate(TASK_ID, false);
        assertThat(oTeeChallenge).isPresent();
        assertThat(oTeeChallenge.get().getCredentials().getPrivateKey()).isEqualTo(ENC_PRIVATE);
        verify(encryptionService, never()).decrypt(anyString());

        verify(teeChallengeMeasuredSecretService, times(1)).newlyAddedSecret();
        verify(ethereumCredentialsMeasuredSecretService, times(1)).newlyAddedSecret();
    }

    @Test
    void shouldCreateNewChallengeAndDecryptKeys() throws Exception {
        TeeChallenge encryptedTeeChallengeStub = getEncryptedTeeChallengeStub();
        when(teeChallengeRepository.findByTaskId(TASK_ID)).thenReturn(Optional.empty());
        when(encryptionService.encrypt(anyString())).thenReturn(ENC_PRIVATE);
        when(teeChallengeRepository.save(any())).thenReturn(encryptedTeeChallengeStub);
        when(encryptionService.decrypt(anyString())).thenReturn(PLAIN_PRIVATE);

        Optional<TeeChallenge> oTeeChallenge = teeChallengeService.getOrCreate(TASK_ID, true);
        assertThat(oTeeChallenge).isPresent();
        assertThat(oTeeChallenge.get().getCredentials().getPrivateKey()).isEqualTo(PLAIN_PRIVATE);

        verify(teeChallengeMeasuredSecretService, times(1)).newlyAddedSecret();
        verify(ethereumCredentialsMeasuredSecretService, times(1)).newlyAddedSecret();
    }

    @Test
    void shouldEncryptChallengeKeys() throws Exception {
        TeeChallenge teeChallenge = new TeeChallenge(TASK_ID);
        when(encryptionService.encrypt(anyString())).thenReturn(ENC_PRIVATE);
        teeChallengeService.encryptChallengeKeys(teeChallenge);

        assertThat(teeChallenge.getCredentials().getPrivateKey()).isEqualTo(ENC_PRIVATE);
    }

    @Test
    void shouldDecryptChallengeKeys() throws Exception {
        TeeChallenge teeChallenge = new TeeChallenge(TASK_ID);
        teeChallenge.getCredentials().setEncrypted(true);
        when(encryptionService.decrypt(anyString())).thenReturn(PLAIN_PRIVATE);

        teeChallengeService.decryptChallengeKeys(teeChallenge);
        assertThat(teeChallenge.getCredentials().getPrivateKey()).isEqualTo(PLAIN_PRIVATE);
    }
}
