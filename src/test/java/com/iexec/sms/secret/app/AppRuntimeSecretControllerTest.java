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

import static org.mockito.Mockito.*;

class AppRuntimeSecretControllerTest {
    private static final String AUTHORIZATION = "authorization";
    private static final String APP_ADDRESS = "appAddress";
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

    // region addAppRuntimeSecret

    @Test
    void shouldAddSecret() {
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

        ResponseEntity<String> result = appRuntimeSecretController.addAppRuntimeSecret(
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
    void shouldNotAddSecretSinceNotSignedByOwner() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);


        ResponseEntity<String> result = appRuntimeSecretController.addAppRuntimeSecret(
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
    void shouldNotAddSecretSinceSecretAlreadyExists() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(true);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRuntimeSecret(
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
    void shouldNotAddSecretSinceSecretValueTooLong() {
        long secretIndex = 0;
        String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(false);

        ResponseEntity<String> result = appRuntimeSecretController.addAppRuntimeSecret(
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
    void shouldAddMaxSizeSecret() {
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

        ResponseEntity<String> result = appRuntimeSecretController.addAppRuntimeSecret(
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

    // region isAppRuntimeSecretPresent
    @Test
    void secretShouldExist() {
        long secretIndex = 0;
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(true);

        ResponseEntity<Void> result =
                appRuntimeSecretController.isAppRuntimeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
    }

    @Test
    void secretShouldNotExist() {
        long secretIndex = 0;
        when(teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex))
                .thenReturn(false);

        ResponseEntity<Void> result =
                appRuntimeSecretController.isAppRuntimeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.notFound().build());
        verify(teeTaskRuntimeSecretService, times(1))
                .isSecretPresent(DeployedObjectType.APPLICATION, APP_ADDRESS, OwnerRole.APPLICATION_DEVELOPER, null, secretIndex);
    }
    // endregion

    // region setAppRequestersAppRuntimeSecretCount
    @Test
    void shouldSetAppRequestersRuntimeSecretCount() {
        int secretCount = 10;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount))
                .thenReturn(true);

        ResponseEntity<String> result = appRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
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

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(false);

        ResponseEntity<String> result = appRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
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

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(true);


        ResponseEntity<String> result = appRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
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

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(false);

        ResponseEntity<String> result = appRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body("Secret count cannot be null."));
        verify(teeTaskRuntimeSecretCountService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSecretCountIsNegative() {
        int secretCount = -1;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(teeTaskRuntimeSecretCountService.isAppRuntimeSecretCountPresent(APP_ADDRESS, OwnerRole.REQUESTER))
                .thenReturn(false);
        when(teeTaskRuntimeSecretCountService.setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount))
                .thenReturn(false);

        ResponseEntity<String> result = appRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body("Secret count should be positive. Can't accept value -1"));
        verify(teeTaskRuntimeSecretCountService, times(1))
                .setAppRuntimeSecretCount(APP_ADDRESS, OwnerRole.REQUESTER, secretCount);
    }
    // endregion
}