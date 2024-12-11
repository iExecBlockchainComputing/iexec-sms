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

import com.iexec.common.web.ApiResponseBody;
import com.iexec.sms.api.SmsClient;
import com.iexec.sms.authorization.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static com.iexec.sms.Web3jUtils.createEthereumAddress;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppComputeSecretControllerTest {
    private static final String AUTHORIZATION = "authorization";
    private static final String COMMON_SECRET_VALUE = "I'm a secret.";
    private static final String EXACT_MAX_SIZE_SECRET_VALUE = new String(new byte[4096]);
    private static final String TOO_LONG_SECRET_VALUE = new String(new byte[4097]);
    private static final String CHALLENGE = "challenge";
    private static final ApiResponseBody<String, List<String>> INVALID_AUTHORIZATION_PAYLOAD = createErrorResponse("Invalid authorization");

    @Mock
    TeeTaskComputeSecretService teeTaskComputeSecretService;

    @Mock
    AuthorizationService authorizationService;

    @InjectMocks
    AppComputeSecretController appComputeSecretController;

    // region addAppDeveloperAppComputeSecret

    @Test
    void shouldCallAddApplicationDeveloperAppComputeSecret() {
        final String appAddress = createEthereumAddress();
        final String secretValue = COMMON_SECRET_VALUE;

        requireAuthorizedAppDeveloper(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
        when(teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
    }

    // endregion

    // region addApplicationDeveloperAppComputeSecret

    @Test
    void shouldAddAppDeveloperSecret() {
        final String appAddress = createEthereumAddress();
        final String secretValue = COMMON_SECRET_VALUE;

        requireAuthorizedAppDeveloper(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
        when(teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addApplicationDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceNotSignedByOwner() {
        final String appAddress = createEthereumAddress();
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, appAddress))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addApplicationDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));

        verify(teeTaskComputeSecretService, never())
                .encryptAndSaveSecret(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretAlreadyExists() {
        final String appAddress = createEthereumAddress();
        final String secretValue = COMMON_SECRET_VALUE;

        requireAuthorizedAppDeveloper(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
        when(teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addApplicationDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Secret already exists")));

        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretValueTooLong() {
        final String appAddress = createEthereumAddress();
        final String secretValue = TOO_LONG_SECRET_VALUE;

        requireAuthorizedAppDeveloper(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addApplicationDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(createErrorResponse("Secret size should not exceed 4 Kb")));

        verifyNoInteractions(teeTaskComputeSecretService);
    }

    @Test
    void shouldAddMaxSizeAppDeveloperSecret() {
        final String appAddress = createEthereumAddress();
        final String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        requireAuthorizedAppDeveloper(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
        when(teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addApplicationDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX, secretValue);
    }

    // endregion

    // region isAppDeveloperAppComputeSecretPresent

    @ParameterizedTest
    @ValueSource(strings = {"bad-secret-index", "-1"})
    void isAppDeveloperAppComputeSecretPresentShouldFail(final String secretIndex) {
        final String appAddress = createEthereumAddress();
        final ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);
        assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    @Test
    void isAppDeveloperAppComputeSecretPresentShouldFailWhenIndexGreaterThanOne() {
        final String secretIndex = "2";
        final String appAddress = createEthereumAddress();
        final ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);
        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(AppComputeSecretController.SECRET_NOT_FOUND_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    @Test
    void shouldCallIsApplicationDeveloperAppComputeSecretPresent() {
        final String appAddress = createEthereumAddress();
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, SmsClient.APP_DEVELOPER_SECRET_INDEX);

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX);
        verifyNoInteractions(authorizationService);
    }

    // endregion

    // region isApplicationDeveloperAppComputeSecretPresent

    @Test
    void appDeveloperSecretShouldExist() {
        final String appAddress = createEthereumAddress();
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isApplicationDeveloperAppComputeSecretPresent(appAddress);

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void appDeveloperSecretShouldNotExist() {
        final String appAddress = createEthereumAddress();
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isApplicationDeveloperAppComputeSecretPresent(appAddress);

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(AppComputeSecretController.SECRET_NOT_FOUND_MSG)));
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", SmsClient.APP_DEVELOPER_SECRET_INDEX);
        verifyNoInteractions(authorizationService);
    }

    // endregion

    // region addRequesterAppComputeSecret
    @Test
    void shouldAddRequesterSecret() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "valid-requester-secret";
        final String secretValue = COMMON_SECRET_VALUE;

        requireAuthorizedRequester(requesterAddress, secretKey, secretValue);
        when(teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey, secretValue))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress);
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceNotSignedByRequester() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "not-signed-secret";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress);
        verifyNoInteractions(teeTaskComputeSecretService);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretAlreadyExists() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "secret-already-exists";
        final String secretValue = COMMON_SECRET_VALUE;

        requireAuthorizedRequester(requesterAddress, secretKey, secretValue);
        when(teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey, secretValue))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Secret already exists")));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress);
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this-is-a-really-long-key-with-far-too-many-characters-in-its-name",
            "this-is-a-key-with-invalid-characters:!*~"
    })
    void shouldNotAddRequesterSecretSinceInvalidSecretKey(String secretKey) {
        final String requesterAddress = createEthereumAddress();

        requireAuthorizedRequester(requesterAddress, secretKey, COMMON_SECRET_VALUE);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                COMMON_SECRET_VALUE
        );

        assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_KEY_FORMAT_MSG)));
        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, COMMON_SECRET_VALUE);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress);
        verifyNoInteractions(teeTaskComputeSecretService);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretValueTooLong() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "too-long-secret-value";
        final String secretValue = TOO_LONG_SECRET_VALUE;

        requireAuthorizedRequester(requesterAddress, secretKey, secretValue);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.badRequest().body(createErrorResponse("Secret size should not exceed 4 Kb")));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress);
        verifyNoInteractions(teeTaskComputeSecretService);
    }

    @Test
    void shouldAddMaxSizeRequesterSecret() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "max-size-secret-value";
        final String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        requireAuthorizedRequester(requesterAddress, secretKey, secretValue);
        when(teeTaskComputeSecretService
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey, secretValue))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress);
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey, secretValue);
    }
    // endregion

    // region isRequesterAppComputeSecretPresent
    @Test
    void requesterSecretShouldExist() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "exist";
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(requesterAddress, secretKey);

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey);
    }

    @Test
    void requesterSecretShouldNotExist() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "empty";
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(requesterAddress, secretKey);

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(AppComputeSecretController.SECRET_NOT_FOUND_MSG)));
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this-is-a-really-long-key-with-far-too-many-characters-in-its-name",
            "this-is-a-key-with-invalid-characters:!*~"
    })
    void shouldNotReadRequesterSecretSinceInvalidSecretKey(String secretKey) {
        final String requesterAddress = createEthereumAddress();
        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.isRequesterAppComputeSecretPresent(
                requesterAddress,
                secretKey
        );

        assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_KEY_FORMAT_MSG)));
        verifyNoInteractions(teeTaskComputeSecretService);
    }
    // endregion

    private static <T> ApiResponseBody<T, List<String>> createErrorResponse(String... errorMessages) {
        return ApiResponseBody.<T, List<String>>builder().error(Arrays.asList(errorMessages)).build();
    }

    private void requireAuthorizedAppDeveloper(String appAddress, String secretIndex, String secretValue) {
        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, appAddress))
                .thenReturn(true);
    }

    private void requireAuthorizedRequester(String requesterAddress, String secretKey, String secretValue) {
        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress))
                .thenReturn(true);
    }

}
