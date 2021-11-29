package com.iexec.sms.secret.app;

import com.iexec.sms.authorization.AuthorizationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

class AppRuntimeSecretControllerTest {
    private static final String AUTHORIZATION = "authorization";
    private static final String APP_ADDRESS = "appAddress";
    private static final String REQUESTER_ADDRESS = "requesterAddress";
    private static final String COMMON_SECRET_VALUE = "I'm a secret.";
    private static final String EXACT_MAX_SIZE_SECRET_VALUE = new String(new byte[4096]);
    private static final String TOO_LONG_SECRET_VALUE = new String(new byte[4097]);
    private static final String CHALLENGE = "challenge";

    @Mock
    TeeTaskRuntimeSecretService teeTaskRuntimeSecretService;

    @Mock
    TeeTaskRuntimeSecretCountService teeTaskRuntimeSecretCountService;

    @Mock
    AuthorizationService authorizationService;

    @InjectMocks
    AppRuntimeSecretController appRuntimeSecretController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region addAppDeveloperAppRuntimeSecret

    @Test
    void shouldAddAppDeveloperSecret() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(false);
        doReturn(true).when(teeTaskRuntimeSecretService)
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex, secretValue);

        ResponseEntity<String> result = appRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
        verify(teeTaskRuntimeSecretService, times(1))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceNotSignedByOwner() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);


        ResponseEntity<String> result = appRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        verify(teeTaskRuntimeSecretService, times(0))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretAlreadyExists() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(true);

        ResponseEntity<String> result = appRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());

        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddAppDeveloperSecretSinceSecretValueTooLong() {
        long secretIndex = 0;
        String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(false);

        ResponseEntity<String> result = appRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());

        verify(teeTaskRuntimeSecretService, times(0))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex, secretValue);
    }

    @Test
    void shouldAddMaxSizeAppDeveloperSecret() {
        long secretIndex = 0;
        String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(false);
        doReturn(true).when(teeTaskRuntimeSecretService)
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex, secretValue);

        ResponseEntity<String> result = appRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
        verify(teeTaskRuntimeSecretService, times(1))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex, secretValue);
    }

    // endregion

    // region isAppDeveloperAppRuntimeSecretPresent
    @Test
    void appDeveloperSecretShouldExist() {
        long secretIndex = 0;
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(true);

        ResponseEntity<Void> result =
                appRuntimeSecretController.isAppDeveloperAppRuntimeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
    }

    @Test
    void appDeveloperSecretShouldNotExist() {
        long secretIndex = 0;
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(false);

        ResponseEntity<Void> result =
                appRuntimeSecretController.isAppDeveloperAppRuntimeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.notFound().build());
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
    }
    // endregion

    // region setRequesterSecretCountForApp
    @Test
    void shouldSetAppRequestersRuntimeSecretCount() {
        int secretCount = 10;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount))
                .thenReturn(true);

        ResponseEntity<Map<String, String>> result = appRuntimeSecretController.setRequesterSecretCountForApp(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskRuntimeSecretCountService, times(1))
                .setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSignedByOwner() {
        int secretCount = 10;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(false);

        ResponseEntity<Map<String, String>> result = appRuntimeSecretController.setRequesterSecretCountForApp(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        verify(teeTaskRuntimeSecretCountService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceSecretCountAlreadyExists() {
        int secretCount = 1;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(true);


        ResponseEntity<Map<String, String>> result = appRuntimeSecretController.setRequesterSecretCountForApp(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result)
                .isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());
        verify(teeTaskRuntimeSecretCountService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSecretCountIsNull() {
        Integer secretCount = null;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(false);

        ResponseEntity<Map<String, String>> result = appRuntimeSecretController.setRequesterSecretCountForApp(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body(Map.of("error", "Secret count cannot be null.")));
        verify(teeTaskRuntimeSecretCountService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSecretCountIsNegative() {
        int secretCount = -1;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount))
                .thenReturn(false);

        ResponseEntity<Map<String, String>> result = appRuntimeSecretController.setRequesterSecretCountForApp(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body(Map.of("error", "Secret count should be positive. Can't accept value -1")));
        verify(teeTaskRuntimeSecretCountService, times(1))
                .setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount);
    }
    // endregion

    // region addAppRequesterAppRuntimeSecret
    @Test
    void shouldAddRequesterSecret() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskRuntimeSecretCount.builder().secretCount(1).build()));
        doReturn(true).when(teeTaskRuntimeSecretService)
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService, times(1))
                .getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(1)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(1))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceNotSignedByRequester() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(false);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        verify(authorizationService, times(1))
                .getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(0))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(0)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretAlreadyExists() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(true);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());

        verify(authorizationService, times(1))
                .getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(0)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretValueTooLong() {
        long secretIndex = 0;
        String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());

        verify(authorizationService, times(0))
                .getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(0))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(0))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(0)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldAddMaxSizeRequesterSecret() {
        long secretIndex = 0;
        String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskRuntimeSecretCount.builder().secretCount(1).build()));
        doReturn(true).when(teeTaskRuntimeSecretService)
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(authorizationService, times(1))
                .getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(1)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(1))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    // TODO: remove following test once more than 1 secret are allowed
    @Test
    void shouldNotAddRequesterSecretSinceIndexTooBig() {
        long secretIndex = 1;
        String secretValue = COMMON_SECRET_VALUE;

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().build());

        verify(authorizationService, times(0))
                .getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(0))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(0))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(0)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceNegativeIndex() {
        long secretIndex = -1;
        String secretValue = COMMON_SECRET_VALUE;

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().build());

        verify(authorizationService, times(0))
                .getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(0))
                .isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(0))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(0)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretCountNotSet() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(Optional.empty());
        doReturn(true).when(teeTaskRuntimeSecretService)
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().build());
        verify(authorizationService, times(1)).
                getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1)).
                isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(1)).
                isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(1)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddRequesterSecretSinceSecretIndexBiggerThanMaxAllowedSecrets() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(Optional.of(TeeTaskRuntimeSecretCount.builder().secretCount(0).build()));
        doReturn(true).when(teeTaskRuntimeSecretService)
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRequesterAppRuntimeSecret(
                AUTHORIZATION,
                REQUESTER_ADDRESS,
                APP_ADDRESS,
                secretIndex,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.badRequest().build());
        verify(authorizationService, times(1)).
                getChallengeForSetAppRequesterAppRuntimeSecret(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex, secretValue);
        verify(authorizationService, times(1)).
                isSignedByHimself(CHALLENGE, AUTHORIZATION, REQUESTER_ADDRESS);
        verify(teeTaskRuntimeSecretService, times(1)).
                isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
        verify(teeTaskRuntimeSecretCountService, times(1)).
                getAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER);
        verify(teeTaskRuntimeSecretService, times(0))
                .encryptAndSaveSecret(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex, secretValue);
    }
    // endregion

    // region isAppRequesterAppRuntimeSecretPresent
    @Test
    void requesterSecretShouldExist() {
        long secretIndex = 0;
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(true);

        ResponseEntity<Void> result =
                appRuntimeSecretController.isAppRequesterAppRuntimeSecretPresent(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
    }

    @Test
    void requesterSecretShouldNotExist() {
        long secretIndex = 0;
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex))
                .thenReturn(false);

        ResponseEntity<Void> result =
                appRuntimeSecretController.isAppRequesterAppRuntimeSecretPresent(REQUESTER_ADDRESS, APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.notFound().build());
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.REQUESTER, REQUESTER_ADDRESS, secretIndex);
    }
    // endregion
}