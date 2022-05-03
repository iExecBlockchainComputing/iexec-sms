/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.api;

import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.common.tee.TeeWorkflowSharedConfiguration;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.web.ApiResponseBody;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SmsServiceTest {

    private static final String AUTHORIZATION = "authorization";
    private static final String ADDRESS = "address";
    private static final String SECRET_INDEX = "index";
    private static final String SECRET_VALUE = "value";
    private static final String SESSION_ID = "sessionId";

    @Mock
    private SmsClient smsClient;

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    public void preflight() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldAddAppDeveloperAppComputeSecret() {}

    @Test
    void shouldReturnFalseOnAddAppDeveloperAppComputeSecretFailure() {
        when(smsClient.addAppDeveloperAppComputeSecret(AUTHORIZATION, ADDRESS, SECRET_VALUE))
                .thenThrow(FeignException.class);
        assertThat(smsService.addAppDeveloperAppComputeSecret(AUTHORIZATION, ADDRESS, SECRET_VALUE))
                .isFalse();
    }

    @Test
    void shouldReturnTrueOnAddAppDeveloperAppComputeSecretSuccess() {
        when(smsClient.addAppDeveloperAppComputeSecret(AUTHORIZATION, ADDRESS, SECRET_VALUE))
                .thenReturn("");
        assertThat(smsService.addAppDeveloperAppComputeSecret(AUTHORIZATION, ADDRESS, SECRET_VALUE))
                .isTrue();
    }

    @Test
    void shouldReturnFalseIfAppDeveloperAppComputeSecretMissing() {
        when(smsClient.isAppDeveloperAppComputeSecretPresent(
                ADDRESS, SECRET_INDEX)).thenThrow(FeignException.class);
        assertThat(smsService.isAppDeveloperAppComputeSecretPresent(ADDRESS, SECRET_INDEX)).isFalse();
    }

    @Test
    void shouldReturnTrueIfAppDeveloperAppComputeSecretPresent() {
        when(smsClient.isAppDeveloperAppComputeSecretPresent(
                ADDRESS, SECRET_INDEX)).thenReturn("");
        assertThat(smsService.isAppDeveloperAppComputeSecretPresent(ADDRESS, SECRET_INDEX)).isTrue();
    }

    @Test
    void shouldReturnNullOnGetMaxSecretCountFailure() {
        when(smsClient.getMaxRequesterSecretCountForAppCompute(ADDRESS)).thenThrow(FeignException.class);
        assertThat(smsService.getMaxRequesterSecretCountForAppCompute(ADDRESS)).isNull();
    }

    @Test
    void shouldReturnValueOnGetMaxSecretCountSuccess() {
        int definedSecretCount = 3;
        ApiResponseBody<Integer> response = ApiResponseBody.<Integer>builder()
                .data(definedSecretCount)
                .build();
        when(smsClient.getMaxRequesterSecretCountForAppCompute(ADDRESS)).thenReturn(response);
        assertThat(smsService.getMaxRequesterSecretCountForAppCompute(ADDRESS)).isEqualTo(definedSecretCount);
    }

    @Test
    void shouldReturnFalseOnSetMaxSecretCountFailure() {
        when(smsClient.setMaxRequesterSecretCountForAppCompute(AUTHORIZATION, ADDRESS, -1))
                .thenThrow(FeignException.class);
        assertThat(smsService.setMaxRequesterSecretCountForAppCompute(AUTHORIZATION, ADDRESS, -1))
                .isFalse();
    }

    @Test
    void shouldReturnTrueOnSetMaxSecretCountSuccess() {
        when(smsClient.setMaxRequesterSecretCountForAppCompute(AUTHORIZATION, ADDRESS, 1))
                .thenReturn("");
        assertThat(smsService.setMaxRequesterSecretCountForAppCompute(AUTHORIZATION, ADDRESS, 1))
                .isTrue();
    }

    @Test
    void shouldReturnFalseOnAddRequesterAppComputeSecretFailure() {
        when(smsClient.addRequesterAppComputeSecret(AUTHORIZATION, ADDRESS, SECRET_INDEX, SECRET_VALUE))
                .thenThrow(FeignException.class);
        assertThat(smsService.addRequesterAppComputeSecret(AUTHORIZATION, ADDRESS, SECRET_INDEX, SECRET_VALUE))
                .isFalse();
    }

    @Test
    void shouldReturnSuccessOnAddRequesterAppComputeSecretSuccess() {
        when(smsClient.addRequesterAppComputeSecret(AUTHORIZATION, ADDRESS, SECRET_INDEX, SECRET_VALUE))
                .thenReturn("");
        assertThat(smsService.addRequesterAppComputeSecret(AUTHORIZATION, ADDRESS, SECRET_INDEX, SECRET_VALUE))
                .isTrue();
    }

    @Test
    void shouldReturnFalseOnIsRequesterAppComputeSecretPresentFailure() {
        when(smsClient.isRequesterAppComputeSecretPresent(
                ADDRESS, SECRET_INDEX)).thenThrow(FeignException.class);
        assertThat(smsService.isRequesterAppComputeSecretPresent(ADDRESS, SECRET_INDEX)).isFalse();
    }

    @Test
    void shouldReturnTrueOnIsRequesterAppComputeSecretPresentFailure() {
        when(smsClient.isRequesterAppComputeSecretPresent(
                ADDRESS, SECRET_INDEX)).thenReturn("");
        assertThat(smsService.isRequesterAppComputeSecretPresent(ADDRESS, SECRET_INDEX)).isTrue();
    }

    @Test
    void shouldReturnFalseOnSetWeb2Failure() {
        when(smsClient.setWeb2Secret(AUTHORIZATION, ADDRESS, SECRET_INDEX, SECRET_VALUE))
                .thenThrow(FeignException.class);
        assertThat(smsService.setWeb2Secret(AUTHORIZATION, ADDRESS, SECRET_INDEX, SECRET_VALUE)).isFalse();
    }

    @Test
    void shouldReturnTrueOnSetWeb2Success() {
        when(smsClient.setWeb2Secret(AUTHORIZATION, ADDRESS, SECRET_INDEX, SECRET_VALUE))
                .thenReturn("");
        assertThat(smsService.setWeb2Secret(AUTHORIZATION, ADDRESS, SECRET_INDEX, SECRET_VALUE)).isTrue();
    }

    @Test
    void shouldReturnFalseOnSetWeb3Failure() {
        when(smsClient.setWeb3Secret(AUTHORIZATION, ADDRESS, SECRET_VALUE))
                .thenThrow(FeignException.class);
        assertThat(smsService.setWeb3Secret(AUTHORIZATION, ADDRESS, SECRET_VALUE)).isFalse();
    }

    @Test
    void shouldReturnFalseOnSetWeb3Success() {
        when(smsClient.setWeb3Secret(AUTHORIZATION, ADDRESS, SECRET_VALUE))
                .thenReturn("");
        assertThat(smsService.setWeb3Secret(AUTHORIZATION, ADDRESS, SECRET_VALUE)).isTrue();
    }

    // core

    @Test
    void shouldGenerateEnclaveChallenge() {
        when(smsClient.generateTeeChallenge("")).thenReturn("publicKey");
        assertThat(smsService.getEnclaveChallenge("", true)).isEqualTo(Optional.of("publicKey"));
        verify(smsClient).generateTeeChallenge(anyString());
    }

    @Test
    void shouldNotGenerateEnclaveChallengeIfKeyNullOrEmpty() {
        when(smsClient.generateTeeChallenge("0x1"))
                .thenReturn(null)
                .thenReturn("");
        assertThat(smsService.getEnclaveChallenge("0x1", true)).isEqualTo(Optional.empty());
        assertThat(smsService.getEnclaveChallenge("0x1", true)).isEqualTo(Optional.empty());
        verify(smsClient, times(2)).generateTeeChallenge(anyString());
    }

    @Test
    void shouldGetEmptyEnclaveChallengeIfNotTee() {
        assertThat(smsService.getEnclaveChallenge("", false))
                .isEqualTo(Optional.of(BytesUtils.EMPTY_ADDRESS));
        verifyNoInteractions(smsClient);
    }

    @Test
    void shouldGetEmptyEnclaveChallengeOnFeignException() {
        when(smsClient.generateTeeChallenge("0x1")).thenThrow(FeignException.class);
        assertThat(smsService.getEnclaveChallenge("0x1", true)).isEqualTo(Optional.empty());
        verify(smsClient).generateTeeChallenge(anyString());
    }

    // worker

    @Test
    void shouldCreateTeeSession() {
        WorkerpoolAuthorization wpAuthorization = WorkerpoolAuthorization.builder()
                .chainTaskId("0x1")
                .build();
        when(smsClient.generateTeeSession(AUTHORIZATION, wpAuthorization)).thenReturn(SESSION_ID);
        assertThat(smsService.createTeeSession(AUTHORIZATION, wpAuthorization)).isEqualTo(SESSION_ID);
        verify(smsClient).generateTeeSession(any(), any());
    }

    @Test
    void shouldGetEmptyTeeSessionIdOnFeignException() {
        WorkerpoolAuthorization wpAuthorization = WorkerpoolAuthorization.builder()
                .chainTaskId("0x2")
                .build();
        when(smsClient.generateTeeSession(AUTHORIZATION, wpAuthorization)).thenThrow(FeignException.class);
        assertThat(smsService.createTeeSession(AUTHORIZATION, wpAuthorization)).isEmpty();
        verify(smsClient).generateTeeSession(any(), any());
    }

    @Test
    void shouldGetSconeCasUrl() {
        when(smsClient.getSconeCasUrl()).thenReturn("localhost");
        assertThat(smsService.getSconeCasUrl()).isEqualTo("localhost");
        verify(smsClient).getSconeCasUrl();
    }

    @Test
    void shouldGetEmptyCasUrlOnFeignException() {
        when(smsClient.getSconeCasUrl()).thenThrow(FeignException.class);
        assertThat(smsService.getSconeCasUrl()).isEqualTo("");
        verify(smsClient).getSconeCasUrl();
    }

    @Test
    void shouldGetCachedTeeWorkflowConfigurationAfterFirstSuccess() {
        TeeWorkflowSharedConfiguration teeWorkflowConfiguration = TeeWorkflowSharedConfiguration.builder().build();
        when(smsClient.getTeeWorkflowConfiguration())
                .thenReturn(teeWorkflowConfiguration)
                .thenReturn(teeWorkflowConfiguration)
                .thenThrow(FeignException.class);
        for (int i = 0 ; i < 5 ; i++) {
            TeeWorkflowSharedConfiguration config = smsService.getTeeWorkflowConfiguration();
            assertThat(config).usingRecursiveComparison().isEqualTo(teeWorkflowConfiguration);
        }
        // Check the client API was called exactly one time
        verify(smsClient).getTeeWorkflowConfiguration();
    }

    @Test
    void shouldGetNullTeeWorkflowConfigurationOnFeignExceptionUntilFirstSuccess() {
        TeeWorkflowSharedConfiguration teeWorkflowConfiguration = TeeWorkflowSharedConfiguration.builder().build();
        when(smsClient.getTeeWorkflowConfiguration())
                .thenThrow(FeignException.class)
                .thenThrow(FeignException.class)
                .thenReturn(teeWorkflowConfiguration);
        assertThat(smsService.getTeeWorkflowConfiguration()).isNull();
        assertThat(smsService.getTeeWorkflowConfiguration()).isNull();
        for (int i = 0; i < 3; i++) {
            TeeWorkflowSharedConfiguration config = smsService.getTeeWorkflowConfiguration();
            assertThat(config).usingRecursiveComparison().isEqualTo(teeWorkflowConfiguration);
        }
        verify(smsClient, times(3)).getTeeWorkflowConfiguration();
    }

}
