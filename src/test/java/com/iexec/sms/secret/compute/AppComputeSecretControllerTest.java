package com.iexec.sms.secret.compute;

import com.iexec.common.web.ApiResponseBody;
import com.iexec.sms.authorization.AuthorizationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.web3j.crypto.Keys;

import java.util.Arrays;
import java.util.List;

import static com.iexec.sms.Web3jUtils.getEthereumAddress;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region addAppDeveloperAppComputeSecret

    @Test
    void shouldAddAppDeveloperSecret() {
        final String appAddress = getEthereumAddress();
        final String checksumAppAddress = Keys.toChecksumAddress(appAddress);
        final String secretIndex = "0";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(checksumAppAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, checksumAppAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verify(teeTaskComputeSecretService, times(1))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceNotSignedByOwner() {
        final String appAddress = getEthereumAddress();
        final String checksumAppAddress = Keys.toChecksumAddress(appAddress);
        final String secretIndex = "0";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(checksumAppAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, checksumAppAddress))
                .thenReturn(false);


        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));

        verify(teeTaskComputeSecretService, never())
                .isSecretPresent(any(), any(), any(), any(), any());
        verify(teeTaskComputeSecretService, never())
                .encryptAndSaveSecret(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretAlreadyExists() {
        final String appAddress = getEthereumAddress();
        final String checksumAppAddress = Keys.toChecksumAddress(appAddress);
        final String secretIndex = "0";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(checksumAppAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, checksumAppAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Secret already exists")));

        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verify(teeTaskComputeSecretService, never())
                .encryptAndSaveSecret(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretValueTooLong() {
        final String appAddress = getEthereumAddress();
        final String checksumAppAddress = Keys.toChecksumAddress(appAddress);
        final String secretIndex = "0";
        final String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(checksumAppAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, checksumAppAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(createErrorResponse("Secret size should not exceed 4 Kb")));

        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    // TODO enable this test when supporting multiple application developer secrets
    @Test
    @Disabled
    void shouldNotAddAppDeveloperSecretSinceBadSecretIndexFormat() {
        final String appAddress = getEthereumAddress();
        final String secretIndex = "bad-secret-index";
        final String secretValue = COMMON_SECRET_VALUE;

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                //secretIndex, // TODO uncomment this when supporting multiple application developer secrets
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    // TODO enable this test when supporting multiple application developer secrets
    @Test
    @Disabled
    void shouldNotAddAppDeveloperSecretSinceBadSecretValue() {
        final String appAddress = getEthereumAddress();
        final String secretIndex = "-10";
        final String secretValue = COMMON_SECRET_VALUE;

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                //secretIndex, // TODO uncomment this when supporting multiple application developer secrets
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    @Test
    void shouldAddMaxSizeAppDeveloperSecret() {
        final String appAddress = getEthereumAddress();
        final String checksumAppAddress = Keys.toChecksumAddress(appAddress);
        final String secretIndex = "0";
        final String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(checksumAppAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, checksumAppAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verify(teeTaskComputeSecretService, times(1))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);
    }

    // endregion

    // region isAppDeveloperAppComputeSecretPresent
    @Test
    void appDeveloperSecretShouldExist() {
        final String secretIndex = "0";
        final String appAddress = getEthereumAddress();
        final String checksumAppAddress = Keys.toChecksumAddress(appAddress);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void appDeveloperSecretShouldNotExist() {
        final String secretIndex = "0";
        final String appAddress = getEthereumAddress();
        final String checksumAppAddress = Keys.toChecksumAddress(appAddress);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Secret not found")));
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, checksumAppAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void isAppDeveloperAppComputeSecretPresentShouldFailWhenIndexNotANumber() {
        final String secretIndex = "bad-secret-index";
        final String appAddress = getEthereumAddress();
        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);
        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    @Test
    void isAppDeveloperAppComputeSecretPresentShouldFailWhenIndexLowerThanZero() {
        final String secretIndex = "-1";
        final String appAddress = getEthereumAddress();
        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);
        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }
    // endregion

    // region addRequesterAppComputeSecret
    @Test
    void shouldAddRequesterSecret() {
        final String requesterAddress = getEthereumAddress();
        final String checksumRequesterAddress = Keys.toChecksumAddress(requesterAddress);
        final String secretKey = "valid-requester-secret";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey))
                .thenReturn(false);
       when(teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey, secretValue))
               .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress);
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey);
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceNotSignedByRequester() {
        final String requesterAddress = getEthereumAddress();
        final String checksumRequesterAddress = Keys.toChecksumAddress(requesterAddress);
        final String secretKey = "not-signed-secret";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress);
        verifyNoInteractions(teeTaskComputeSecretService);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretAlreadyExists() {
        final String requesterAddress = getEthereumAddress();
        final String checksumRequesterAddress = Keys.toChecksumAddress(requesterAddress);
        final String secretKey = "secret-already-exists";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Secret already exists")));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress);
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey);
        verify(teeTaskComputeSecretService, never())
                .encryptAndSaveSecret(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this-is-a-really-long-key-with-far-too-many-characters-in-its-name",
            "this-is-a-key-with-invalid-characters:!*~"
    })
    void shouldNotAddRequesterSecretSinceInvalidSecretKey(String secretKey) {
        final String requesterAddress = getEthereumAddress();

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                COMMON_SECRET_VALUE
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_KEY_FORMAT_MSG)));
        verifyNoInteractions(teeTaskComputeSecretService);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretValueTooLong() {
        final String requesterAddress = getEthereumAddress();
        final String checksumRequesterAddress = Keys.toChecksumAddress(requesterAddress);
        final String secretKey = "too-long-secret-value";
        final String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().body(createErrorResponse("Secret size should not exceed 4 Kb")));

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress);
        verifyNoInteractions(teeTaskComputeSecretService);
    }

    @Test
    void shouldAddMaxSizeRequesterSecret() {
        final String requesterAddress = getEthereumAddress();
        final String checksumRequesterAddress = Keys.toChecksumAddress(requesterAddress);
        final String secretKey = "max-size-secret-value";
        final String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey))
                .thenReturn(false);
        when(teeTaskComputeSecretService
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey, secretValue))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(checksumRequesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, checksumRequesterAddress);
        verify(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey);
        verify(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey, secretValue);
    }
    // endregion

    // region isRequesterAppComputeSecretPresent
    @Test
    void requesterSecretShouldExist() {
        final String requesterAddress = getEthereumAddress();
        final String checksumRequesterAddress = Keys.toChecksumAddress(requesterAddress);
        final String secretKey = "exist";
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(requesterAddress, secretKey);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey);
    }

    @Test
    void requesterSecretShouldNotExist() {
        final String requesterAddress = getEthereumAddress();
        final String checksumRequesterAddress = Keys.toChecksumAddress(requesterAddress);
        final String secretKey = "empty";
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(checksumRequesterAddress, secretKey);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Secret not found")));
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, checksumRequesterAddress, secretKey);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this-is-a-really-long-key-with-far-too-many-characters-in-its-name",
            "this-is-a-key-with-invalid-characters:!*~"
    })
    void shouldNotReadRequesterSecretSinceInvalidSecretKey(String secretKey) {
        final String requesterAddress = getEthereumAddress();
        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.isRequesterAppComputeSecretPresent(
                requesterAddress,
                secretKey
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_KEY_FORMAT_MSG)));
        verifyNoInteractions(teeTaskComputeSecretService);
    }
    // endregion

    private static <T> ApiResponseBody<T, List<String>> createErrorResponse(String... errorMessages) {
        return ApiResponseBody.<T, List<String>>builder().error(Arrays.asList(errorMessages)).build();
    }

}