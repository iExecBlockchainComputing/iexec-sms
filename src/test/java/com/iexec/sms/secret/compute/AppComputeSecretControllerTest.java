package com.iexec.sms.secret.compute;

import com.iexec.common.contract.generated.Ownable;
import com.iexec.common.utils.BytesUtils;
import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.blockchain.IexecHubService;
import com.iexec.common.web.ApiResponse;
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
    private static final ApiResponse<String> INVALID_AUTHORIZATION_PAYLOAD = createErrorResponse("Invalid authorization");
    private static final Ownable APP_CONTRACT = mockAppContract();

    @Mock
    TeeTaskComputeSecretService teeTaskComputeSecretService;

    @Mock
    TeeTaskComputeSecretCountService teeTaskComputeSecretCountService;

    @Mock
    AuthorizationService authorizationService;

    @Mock
    IexecHubService iexecHubService;

    @InjectMocks
    AppComputeSecretController appComputeSecretController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region addAppDeveloperAppComputeSecret

    @Test
    void shouldAddAppDeveloperSecret() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
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
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);


        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
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
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(true);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
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
        long secretIndex = 0;
        String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
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
        long secretIndex = 0;
        String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppComputeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex, secretValue);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addAppDeveloperAppComputeSecret(
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
        long secretIndex = 0;
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(true);

        ResponseEntity<ApiResponse<String>> result =
                appComputeSecretController.isAppDeveloperAppComputeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex);
    }

    @Test
    void appDeveloperSecretShouldNotExist() {
        long secretIndex = 0;
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", secretIndex))
                .thenReturn(false);

        ResponseEntity<ApiResponse<String>> result =
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

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.setMaxRequesterSecretCountForAppCompute(
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

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.setMaxRequesterSecretCountForAppCompute(
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


        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.setMaxRequesterSecretCountForAppCompute(
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

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.setMaxRequesterSecretCountForAppCompute(
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

        final ResponseEntity<ApiResponse<Integer>> result =
                appComputeSecretController.getMaxRequesterSecretCountForAppCompute(APP_ADDRESS);

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .ok(ApiResponse.builder().data(secretCount).build()));
        verify(teeTaskComputeSecretCountService, times(1))
                .getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
    }

    @Test
    void shouldNotGetMaxRequesterSecretCountForAppComputeSinceNotDefined() {
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.empty());

        final ResponseEntity<ApiResponse<Integer>> result =
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
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(1).build()));
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(iexecHubService, times(1))
                .getOwnableContract(APP_ADDRESS);
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(1)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(1))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceNotSignedByRequester() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(false);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_AUTHORIZATION_PAYLOAD));

        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskComputeSecretService, times(0))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(0)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretAlreadyExists() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(1).build()));
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(true);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Secret already exists")));

        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(iexecHubService, times(1))
                .getOwnableContract(APP_ADDRESS);
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(1)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretValueTooLong() {
        long secretIndex = 0;
        String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(1).build()));
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().body(createErrorResponse("Secret size should not exceed 4 Kb")));

        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskComputeSecretService, times(0))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(1)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldAddMaxSizeRequesterSecret() {
        long secretIndex = 0;
        String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(1).build()));
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(iexecHubService, times(1))
                .getOwnableContract(APP_ADDRESS);
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(1)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(1))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    // TODO: remove following test once more than 1 secret are allowed
    @Test
    void shouldNotAddRequesterSecretSinceIndexTooBig() {
        long secretIndex = 1;
        String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(2).build()));

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().body(createErrorResponse("Can't add more than a single app requester secret as of now.")));

        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(iexecHubService, times(1))
                .getOwnableContract(APP_ADDRESS);
        verify(teeTaskComputeSecretService, times(0))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(1)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceNegativeIndex() {
        long secretIndex = -1;
        String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(1).build()));

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().body(createErrorResponse("Negative index are forbidden for app requester secrets.")));

        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(iexecHubService, times(1))
                .getOwnableContract(APP_ADDRESS);
        verify(teeTaskComputeSecretService, times(0))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(1)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretCountNotSet() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.empty());
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().body(createErrorResponse("No secret count has been provided")));
        verify(authorizationService, times(1)).
                getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1)).
                isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        verify(teeTaskComputeSecretService, times(0)).
                isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(1)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretIndexBiggerThanMaxAllowedSecrets() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(APP_CONTRACT);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(0).build()));
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().body(createErrorResponse("Index is greater than allowed secrets count")));
        verify(authorizationService, times(1)).
                getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1)).
                isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(iexecHubService, times(1))
                .getOwnableContract(APP_ADDRESS);
        verify(teeTaskComputeSecretService, times(0)).
                isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(1)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceAppNotExist() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(APP_ADDRESS)).thenReturn(null);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(1).build()));
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("App does not exist")));
        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(iexecHubService, times(1))
                .getOwnableContract(APP_ADDRESS);
        verify(teeTaskComputeSecretService, times(0))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(0)).
                getMaxAppComputeSecretCount(APP_ADDRESS, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceAppAddressEmpty() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;
        final String emptyAddress = BytesUtils.EMPTY_ADDRESS;

        when(authorizationService.getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, emptyAddress, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(iexecHubService.getOwnableContract(emptyAddress)).thenReturn(null);
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, emptyAddress, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskComputeSecretCountService.getMaxAppComputeSecretCount(emptyAddress, SecretOwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskComputeSecretCount.builder().secretCount(1).build()));
        doReturn(true).when(teeTaskComputeSecretService)
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, emptyAddress, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<ApiResponse<String>> result = appComputeSecretController.addRequesterAppComputeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                emptyAddress,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("App does not exist")));
        verify(authorizationService, times(1))
                .getChallengeForSetRequesterAppComputeSecret(REQUESTER_ADDRESS, emptyAddress, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(iexecHubService, times(1))
                .getOwnableContract(emptyAddress);
        verify(teeTaskComputeSecretService, times(0))
                .isSecretPresent(OnChainObjectType.APPLICATION, emptyAddress, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskComputeSecretCountService, times(0)).
                getMaxAppComputeSecretCount(emptyAddress, SecretOwnerRole.REQUESTER);
        verify(teeTaskComputeSecretService, times(0))
                .encryptAndSaveSecret(OnChainObjectType.APPLICATION, emptyAddress, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }
    // endregion

    // region isRequesterAppComputeSecretPresent
    @Test
    void requesterSecretShouldExist() {
        long secretIndex = 0;
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(true);

        ResponseEntity<ApiResponse<String>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
    }

    @Test
    void requesterSecretShouldNotExist() {
        long secretIndex = 0;
        when(teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);

        ResponseEntity<ApiResponse<String>> result =
                appComputeSecretController.isRequesterAppComputeSecretPresent(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Secret not found")));
        verify(teeTaskComputeSecretService, times(1))
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
    }
    // endregion

    private static <T> ApiResponse<T> createErrorResponse(String... errorMessages) {
        return ApiResponse.<T>builder().errors(Arrays.asList(errorMessages)).build();
    }

    private static Ownable mockAppContract() {
        final Ownable appContract = mock(Ownable.class);
        when(appContract.getContractAddress()).thenReturn(APP_ADDRESS);
        return appContract;
    }
}