package com.iexec.sms.secret.app;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.app.owner.AppDeveloperAppRuntimeSecretController;
import com.iexec.sms.secret.app.owner.AppDeveloperAppRuntimeSecretService;
import com.iexec.sms.secret.app.requester.AppRequesterAppRuntimeSecretService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;

class AppDeveloperAppRuntimeSecretControllerTest {
    private static final String AUTHORIZATION = "authorization";
    private static final String APP_ADDRESS = "appAddress";
    private static final String COMMON_SECRET_VALUE = "I'm a secret.";
    private static final String EXACT_MAX_SIZE_SECRET_VALUE = new String(new byte[4096]);
    private static final String TOO_LONG_SECRET_VALUE = new String(new byte[4097]);
    private static final String CHALLENGE = "challenge";

    @Mock
    AppDeveloperAppRuntimeSecretService appDeveloperAppRuntimeSecretService;

    @Mock
    AppRequesterAppRuntimeSecretService appRequesterAppRuntimeSecretService;

    @Mock
    AuthorizationService authorizationService;

    @InjectMocks
    AppDeveloperAppRuntimeSecretController appDeveloperAppRuntimeSecretController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region addApplicationRuntimeSecret

    @Test
    void shouldAddSecret() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appDeveloperAppRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(false);
        doNothing().when(appDeveloperAppRuntimeSecretService).encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);

        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(appDeveloperAppRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperAppRuntimeSecretService, times(1))
                .encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddSecretSinceNotSignedByOwner() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);


        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        verify(appDeveloperAppRuntimeSecretService, times(0))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperAppRuntimeSecretService, times(0))
                .encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddSecretSinceSecretAlreadyExists() {
        long secretIndex = 0;
        final String secretValue = COMMON_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appDeveloperAppRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(true);

        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());

        verify(appDeveloperAppRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperAppRuntimeSecretService, times(0))
                .encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldNotAddSecretSinceSecretValueTooLong() {
        long secretIndex = 0;
        String secretValue = TOO_LONG_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appDeveloperAppRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(false);

        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());

        verify(appDeveloperAppRuntimeSecretService, times(0))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperAppRuntimeSecretService, times(0))
                .encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);
    }

    @Test
    void shouldAddMaxSizeSecret() {
        long secretIndex = 0;
        String secretValue = EXACT_MAX_SIZE_SECRET_VALUE;

        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, secretValue))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appDeveloperAppRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(false);
        doNothing().when(appDeveloperAppRuntimeSecretService).encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);

        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.addAppDeveloperAppRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(appDeveloperAppRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperAppRuntimeSecretService, times(1))
                .encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);
    }

    // endregion

    // region checkApplicationRuntimeSecretExistence
    @Test
    void secretShouldExist() {
        long secretIndex = 0;
        when(appDeveloperAppRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(true);

        ResponseEntity<Void> result =
                appDeveloperAppRuntimeSecretController.isAppDeveloperAppRuntimeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(appDeveloperAppRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
    }

    @Test
    void secretShouldNotExist() {
        long secretIndex = 0;
        when(appDeveloperAppRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(false);

        ResponseEntity<Void> result =
                appDeveloperAppRuntimeSecretController.isAppDeveloperAppRuntimeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.notFound().build());
        verify(appDeveloperAppRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
    }
    // endregion

    // region setAppRequestersRuntimeSecretCount
    @Test
    void shouldSetAppRequestersRuntimeSecretCount() {
        int secretCount = 10;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appRequesterAppRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(false);
        doNothing().when(appRequesterAppRuntimeSecretService)
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);

        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(appRequesterAppRuntimeSecretService, times(1))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSignedByOwner() {
        int secretCount = 10;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);
        when(appRequesterAppRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(false);

        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        verify(appRequesterAppRuntimeSecretService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceSecretCountAlreadyExists() {
        int secretCount = 1;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appRequesterAppRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(true);


        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result)
                .isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());
        verify(appRequesterAppRuntimeSecretService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSecretCountIsNull() {
        Integer secretCount = null;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appRequesterAppRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(false);
        doNothing().when(appRequesterAppRuntimeSecretService)
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);

        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body("Secret count should be positive. Can't accept value null"));
        verify(appRequesterAppRuntimeSecretService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSecretCountIsNegative() {
        int secretCount = -1;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appRequesterAppRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(false);
        doNothing().when(appRequesterAppRuntimeSecretService)
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);

        ResponseEntity<String> result = appDeveloperAppRuntimeSecretController.setAppRequestersAppRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body("Secret count should be positive. Can't accept value -1"));
        verify(appRequesterAppRuntimeSecretService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }
    // endregion
}