package com.iexec.sms.secret.compute;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.common.web.ApiResponseBody;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.*;

class AppComputeSecretControllerTest {
    private static final String AUTHORIZATION = "authorization";
    private static final String APP_ADDRESS = "appAddress";
    private static final String REQUESTER_ADDRESS = "requesterAddress";
    private static final String COMMON_SECRET_VALUE = "I'm a secret.";
    private static final String EXACT_MAX_SIZE_SECRET_VALUE = new String(new byte[4096]);
    private static final String TOO_LONG_SECRET_VALUE = new String(new byte[4097]);
    private static final String CHALLENGE = "challenge";
    private static final ApiResponseBody<String> INVALID_AUTHORIZATION_PAYLOAD = createErrorResponse("Invalid authorization");

    @Mock
    TeeTaskComputeSecretService teeTaskComputeSecretService;

    @Mock
    TeeTaskComputeSecretCountService teeTaskComputeSecretCountService;

    @Mock
    AuthorizationService authorizationService;

    @InjectMocks
    AppComputeSecretController appComputeSecretController;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region addAppDeveloperAppComputeSecret

    @Test
    void shouldAddAppDeveloperSecret() {
        String secretIndex = "0";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verify(teeTaskComputeSecretService, times(1))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceNotSignedByOwner() {
        String secretIndex = "0";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);


        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));

        verify(teeTaskComputeSecretService, times(0))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretAlreadyExists() {
        String secretIndex = "0";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Secret already exists")));

        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretValueTooLong() {
        String secretIndex = "0";
        String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(createErrorResponse("Secret size should not exceed 4 Kb")));

        verify(teeTaskComputeSecretService, times(0))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);
    }

    @Test
    void shouldAddMaxSizeAppDeveloperSecret() {
        String secretIndex = "0";
        String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verify(teeTaskComputeSecretService, times(1))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);
    }

    // endregion

    // region isAppDeveloperAppComputeSecretPresent
    @Test
    void appDeveloperSecretShouldExist() {
        String secretIndex = "0";
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
    }

    @Test
    void appDeveloperSecretShouldNotExist() {
        String secretIndex = "0";
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Secret not found")));
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
    }
    // endregion

    // region setMaxRequesterSecretCountForAppCompute
    @Test
    void shouldSetRequestersComputeSecretCount() {
        int secretCount = 10;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretCountService.isMaxAppComputeSecretCountPresent(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(false);
        when(teeTaskComputeSecretCountService.setMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER, secretCount))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.setMaxRequesterSecretCountForAppCompute(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretCountService, times(1))
                .setMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER, secretCount);
    }

    @Test
    void shouldNotSetRequestersComputeSecretCountSinceNotSignedByOwner() {
        int secretCount = 10;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);
        when(teeTaskComputeSecretCountService.isMaxAppComputeSecretCountPresent(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.setMaxRequesterSecretCountForAppCompute(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));
        verify(teeTaskComputeSecretCountService, times(0))
                .setMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER, secretCount);
    }

    @Test
    void shouldNotSetRequestersComputeSecretCountSinceSecretCountAlreadyExists() {
        int secretCount = 1;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretCountService.isMaxAppComputeSecretCountPresent(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(true);


        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.setMaxRequesterSecretCountForAppCompute(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result)
                .isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Secret count already exists")));
        verify(teeTaskComputeSecretCountService, times(0))
                .setMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER, secretCount);
    }

    @Test
    void shouldNotSetRequestersComputeSecretCountSinceNotSecretCountIsNegative() {
        int secretCount = -1;

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.setMaxRequesterSecretCountForAppCompute(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body(createErrorResponse("Secret count should be positive. Can't accept value -1")));

        verify(authorizationService, times(0))
                .getChallengeForSetRequesterAppComputeSecretCount(APP_ADDRESS, secretCount);
        verify(authorizationService, times(0))
                .isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS);
        verify(teeTaskComputeSecretCountService, times(0))
                .isMaxAppComputeSecretCountPresent(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretCountService, times(0))
                .setMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER, secretCount);
    }
    // endregion

    // region setMaxRequesterSecretCountForAppCompute
    @Test
    void shouldGetMaxRequesterSecretCountForAppCompute() {
        final int secretCount = 10;

        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount
                        .builder()
                        .appAddress(APP_ADDRESS)
                        .secretOwnerRole(SecretOwnerRole.REQUESTER)
                        .secretCount(secretCount)
                        .build())
                );

        final ResponseEntity<ApiResponseBody<Integer>> result =
                appComputeSecretController.getMaxRequesterSecretCountForAppCompute(APP_ADDRESS);

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .ok(ApiResponseBody.builder().data(secretCount).build()));
        verify(teeTaskComputeSecretCountService, times(1))
                .getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
    }

    @Test
    void shouldNotGetMaxRequesterSecretCountForAppComputeSinceNotDefined() {
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.empty());

        final ResponseEntity<ApiResponseBody<Integer>> result =
                appComputeSecretController.getMaxRequesterSecretCountForAppCompute(APP_ADDRESS);

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse("Secret count not found")));
        verify(teeTaskComputeSecretCountService, times(1))
                .getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
    }
    // endregion

    // region addRequesterAppComputeSecret
    @Test
    void shouldAddRequesterSecret() {
        String secretKey = "valid-requester-secret";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey))
                .thenReturn(false);
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey, secretValue);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey);
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey, secretValue);
        verifyNoInteractions(teeTaskComputeSecretCountService);
    }

    @Test
    void shouldNotAddRequesterSecretSinceNotSignedByRequester() {
        String secretKey = "not-signed-secret";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verifyNoInteractions(teeTaskComputeSecretCountService, teeTaskComputeSecretService);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretAlreadyExists() {
        String secretKey = "secret-already-exists";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Secret already exists")));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey);
        verify(teeTaskComputeSecretService, never())
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey, secretValue);
        verifyNoInteractions(teeTaskComputeSecretCountService);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretValueTooLong() {
        String secretKey = "too-long-secret-value";
        String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().body(createErrorResponse("Secret size should not exceed 4 Kb")));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verifyNoInteractions(teeTaskComputeSecretCountService, teeTaskComputeSecretService);
    }

    @Test
    void shouldAddMaxSizeRequesterSecret() {
        String secretKey = "max-size-secret-value";
        String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey))
                .thenReturn(false);
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey, secretValue);

        ResponseEntity<ApiResponseBody<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey);
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey, secretValue);
        verifyNoInteractions(teeTaskComputeSecretCountService);
    }
    // endregion

    // region isRequesterAppComputeSecretPresent
    @Test
    void requesterSecretShouldExist() {
        String secretKey = "exist";
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(REQUESTER_ADDRESS, secretKey);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey);
    }

    @Test
    void requesterSecretShouldNotExist() {
        String secretKey = "empty";
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(REQUESTER_ADDRESS, secretKey);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Secret not found")));
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretKey);
    }
    // endregion

    private static <T> ApiResponseBody<T> createErrorResponse(String... errorMessages) {
        return ApiResponseBody.<T>builder().errors(Arrays.asList(errorMessages)).build();
    }

}