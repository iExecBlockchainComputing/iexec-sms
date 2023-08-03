package com.iexec.sms.secret.compute;

import com.iexec.common.web.ApiResponseBody;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.exception.SecretAlreadyExistsException;
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

import java.util.Arrays;
import java.util.List;

import static com.iexec.sms.Web3jUtils.createEthereumAddress;
import static org.assertj.core.api.Assertions.assertThat;
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
    void shouldAddAppDeveloperSecret() throws SecretAlreadyExistsException {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "1";
        final String secretValue = COMMON_SECRET_VALUE;
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                secretIndex
        );

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, appAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(false);
        when(teeTaskComputeSecretService.addSecret(header, secretValue))
                .thenReturn(new TeeTaskComputeSecret(header, secretValue));

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .addSecret(header, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceNotSignedByOwner() throws SecretAlreadyExistsException {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "1";
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, appAddress))
                .thenReturn(false);


        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));

        verify(teeTaskComputeSecretService, never())
                .isSecretPresent(any());
        verify(teeTaskComputeSecretService, never())
                .addSecret(any(), any());
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretAlreadyExists() throws SecretAlreadyExistsException {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "1";
        final String secretValue = COMMON_SECRET_VALUE;
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                secretIndex
        );

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, appAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.addSecret(any(), any()))
                .thenThrow(new SecretAlreadyExistsException(header));

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        final ResponseEntity<ApiResponseBody<Object, List<String>>> expectedResponse =
                ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(createErrorResponse("Secret already exists"));
        assertThat(result)
                .isEqualTo(expectedResponse);

        verify(teeTaskComputeSecretService)
                .addSecret(any(), any());
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretValueTooLong() {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "1";
        final String secretValue = TOO_LONG_SECRET_VALUE;
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(OnChainObjectType.APPLICATION, appAddress, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, appAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(createErrorResponse("Secret size should not exceed 4 Kb")));

        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    // TODO enable this test when supporting multiple application developer secrets
    @Test
    @Disabled
    void shouldNotAddAppDeveloperSecretSinceBadSecretIndexFormat() {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "bad-secret-index";
        final String secretValue = COMMON_SECRET_VALUE;

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                //secretIndex, // TODO uncomment this when supporting multiple application developer secrets
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    // TODO enable this test when supporting multiple application developer secrets
    @Test
    @Disabled
    void shouldNotAddAppDeveloperSecretSinceBadSecretValue() {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "-10";
        final String secretValue = COMMON_SECRET_VALUE;

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                //secretIndex, // TODO uncomment this when supporting multiple application developer secrets
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    @Test
    void shouldAddMaxSizeAppDeveloperSecret() throws SecretAlreadyExistsException {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "1";
        final String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                secretIndex
        );

        when(authorizationService.getChallengeForSetAppDeveloperAppComputeSecret(appAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, appAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(false);
        when(teeTaskComputeSecretService.addSecret(header, secretValue))
                .thenReturn(new TeeTaskComputeSecret(header, secretValue));

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
                AUTHORIZATION,
                appAddress,
                secretValue
        );

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .addSecret(header, secretValue);
    }

    // endregion

    // region isAppDeveloperAppComputeSecretPresent
    @Test
    void appDeveloperSecretShouldExist() {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "1";
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                secretIndex
        );

        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(header);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void appDeveloperSecretShouldNotExist() {
        final String appAddress = createEthereumAddress();
        final String secretIndex = "1";
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                appAddress,
                SecretOwnerRole.APPLICATION_DEVELOPER,
                "",
                secretIndex
        );

        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Secret not found")));
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(header);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void isAppDeveloperAppComputeSecretPresentShouldFailWhenIndexNotANumber() {
        final String secretIndex = "bad-secret-index";
        final String appAddress = createEthereumAddress();
        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);
        assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }

    @Test
    void isAppDeveloperAppComputeSecretPresentShouldFailWhenIndexLowerThanZero() {
        final String secretIndex = "-1";
        final String appAddress = createEthereumAddress();
        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);
        assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_INDEX_FORMAT_MSG)));
        verifyNoInteractions(authorizationService, teeTaskComputeSecretService);
    }
    // endregion

    // region addRequesterAppComputeSecret
    @Test
    void shouldAddRequesterSecret() throws SecretAlreadyExistsException {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "valid-requester-secret";
        final String secretValue = COMMON_SECRET_VALUE;
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(OnChainObjectType.APPLICATION, "", SecretOwnerRole.REQUESTER, requesterAddress, secretKey);

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(false);
       when(teeTaskComputeSecretService.addSecret(header, secretValue))
               .thenReturn(new TeeTaskComputeSecret(header, secretValue));

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
                .addSecret(header, secretValue);
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
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretAlreadyExists() throws SecretAlreadyExistsException {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "secret-already-exists";
        final String secretValue = COMMON_SECRET_VALUE;
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                "",
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretKey
        );

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.addSecret(any(), any()))
                .thenThrow(new SecretAlreadyExistsException(header));

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                secretValue
        );

        final ResponseEntity<ApiResponseBody<Object, List<String>>> expectedResponse =
                ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(createErrorResponse("Secret already exists"));
        assertThat(result).isEqualTo(expectedResponse);

        verify(authorizationService)
                .getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue);
        verify(authorizationService)
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress);
        verify(teeTaskComputeSecretService)
                .addSecret(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this-is-a-really-long-key-with-far-too-many-characters-in-its-name",
            "this-is-a-key-with-invalid-characters:!*~"
    })
    void shouldNotAddRequesterSecretSinceInvalidSecretKey(String secretKey) {
        final String requesterAddress = createEthereumAddress();

        ResponseEntity<ApiResponseBody<String, List<String>>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                requesterAddress,
                secretKey,
                COMMON_SECRET_VALUE
        );

        assertThat(result).isEqualTo(ResponseEntity.badRequest()
                .body(createErrorResponse(AppComputeSecretController.INVALID_SECRET_KEY_FORMAT_MSG)));
        verifyNoInteractions(teeTaskComputeSecretService);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretValueTooLong() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "too-long-secret-value";
        final String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress))
                .thenReturn(true);

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
    }

    @Test
    void shouldAddMaxSizeRequesterSecret() throws SecretAlreadyExistsException {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "max-size-secret-value";
        final String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                "",
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretKey
        );

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(requesterAddress, secretKey, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, requesterAddress))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(false);
        when(teeTaskComputeSecretService.addSecret(header, secretValue))
                .thenReturn(new TeeTaskComputeSecret(header, secretValue));

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
                .addSecret(header, secretValue);
    }
    // endregion

    // region isRequesterAppComputeSecretPresent
    @Test
    void requesterSecretShouldExist() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "exist";
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                "",
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretKey
        );

        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(true);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(requesterAddress, secretKey);

        assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(header);
    }

    @Test
    void requesterSecretShouldNotExist() {
        final String requesterAddress = createEthereumAddress();
        final String secretKey = "empty";
        final TeeTaskComputeSecretHeader header = new TeeTaskComputeSecretHeader(
                OnChainObjectType.APPLICATION,
                "",
                SecretOwnerRole.REQUESTER,
                requesterAddress,
                secretKey
        );

        when(teeTaskComputeSecretService.isSecretPresent(header))
                .thenReturn(false);

        ResponseEntity<ApiResponseBody<String, List<String>>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(requesterAddress, secretKey);

        assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Secret not found")));
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(header);
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

}