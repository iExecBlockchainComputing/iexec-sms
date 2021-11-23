package com.iexec.sms.secret.app;

import com.iexec.sms.authorization.AuthorizationService;
import com.iexec.sms.secret.app.owner.AppDeveloperRuntimeSecretController;
import com.iexec.sms.secret.app.owner.AppDeveloperRuntimeSecretService;
import com.iexec.sms.secret.app.requester.AppRequesterRuntimeSecretService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;

class AppDeveloperRuntimeSecretControllerTest {
    private static final String AUTHORIZATION = "authorization";
    private static final String APP_ADDRESS = "appAddress";
    private static final String COMMON_SECRET_VALUE = "I'm a secret.";
    private static final String EXACT_MAX_SIZE_SECRET_VALUE = new String(new byte[4096]);
    private static final String TOO_LONG_SECRET_VALUE = new String(new byte[4097]);
    private static final String CHALLENGE = "challenge";

    @Mock
    AppDeveloperRuntimeSecretService appDeveloperRuntimeSecretService;

    @Mock
    AppRequesterRuntimeSecretService appRequesterRuntimeSecretService;

    @Mock
    AuthorizationService authorizationService;

    @InjectMocks
    AppDeveloperRuntimeSecretController appDeveloperRuntimeSecretController;

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
        when(appDeveloperRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(false);
        doNothing().when(appDeveloperRuntimeSecretService).encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);

        ResponseEntity<String> result = appDeveloperRuntimeSecretController.addApplicationRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(appDeveloperRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperRuntimeSecretService, times(1))
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


        ResponseEntity<String> result = appDeveloperRuntimeSecretController.addApplicationRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        verify(appDeveloperRuntimeSecretService, times(0))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperRuntimeSecretService, times(0))
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
        when(appDeveloperRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(true);

        ResponseEntity<String> result = appDeveloperRuntimeSecretController.addApplicationRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());

        verify(appDeveloperRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperRuntimeSecretService, times(0))
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
        when(appDeveloperRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(false);

        ResponseEntity<String> result = appDeveloperRuntimeSecretController.addApplicationRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build());

        verify(appDeveloperRuntimeSecretService, times(0))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperRuntimeSecretService, times(0))
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
        when(appDeveloperRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(false);
        doNothing().when(appDeveloperRuntimeSecretService).encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);

        ResponseEntity<String> result = appDeveloperRuntimeSecretController.addApplicationRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                secretValue
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(appDeveloperRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
        verify(appDeveloperRuntimeSecretService, times(1))
                .encryptAndSaveSecret(APP_ADDRESS, secretIndex, secretValue);
    }

    // endregion

    // region checkApplicationRuntimeSecretExistence
    @Test
    void secretShouldExist() {
        long secretIndex = 0;
        when(appDeveloperRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(true);

        ResponseEntity<Void> result =
                appDeveloperRuntimeSecretController.isApplicationRuntimeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(appDeveloperRuntimeSecretService, times(1))
                .isSecretPresent(APP_ADDRESS, secretIndex);
    }

    @Test
    void secretShouldNotExist() {
        long secretIndex = 0;
        when(appDeveloperRuntimeSecretService.isSecretPresent(APP_ADDRESS, secretIndex))
                .thenReturn(false);

        ResponseEntity<Void> result =
                appDeveloperRuntimeSecretController.isApplicationRuntimeSecretPresent(APP_ADDRESS, secretIndex);

        Assertions.assertThat(result).isEqualTo(ResponseEntity.notFound().build());
        verify(appDeveloperRuntimeSecretService, times(1))
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
        when(appRequesterRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(false);
        doNothing().when(appRequesterRuntimeSecretService)
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);

        ResponseEntity<String> result = appDeveloperRuntimeSecretController.setAppRequestersRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
        verify(appRequesterRuntimeSecretService, times(1))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSignedByOwner() {
        int secretCount = 10;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);
        when(appRequesterRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(false);

        ResponseEntity<String> result = appDeveloperRuntimeSecretController.setAppRequestersRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        verify(appRequesterRuntimeSecretService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceSecretCountAlreadyExists() {
        int secretCount = 1;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appRequesterRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(true);


        ResponseEntity<String> result = appDeveloperRuntimeSecretController.setAppRequestersRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result)
                .isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());
        verify(appRequesterRuntimeSecretService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSecretCountIsNull() {
        Integer secretCount = null;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appRequesterRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(false);
        doNothing().when(appRequesterRuntimeSecretService)
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);

        ResponseEntity<String> result = appDeveloperRuntimeSecretController.setAppRequestersRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body("Secret count should be positive. Can't accept value null"));
        verify(appRequesterRuntimeSecretService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }

    @Test
    void shouldNotSetAppRequestersRuntimeSecretCountSinceNotSecretCountIsNegative() {
        int secretCount = -1;

        when(authorizationService.getChallengeForSetAppRequesterRuntimeSecretCount(APP_ADDRESS, secretCount))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(appRequesterRuntimeSecretService.isAppRuntimeSecretCountPresent(APP_ADDRESS))
                .thenReturn(false);
        doNothing().when(appRequesterRuntimeSecretService)
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);

        ResponseEntity<String> result = appDeveloperRuntimeSecretController.setAppRequestersRuntimeSecretCount(
                AUTHORIZATION,
                APP_ADDRESS,
                secretCount
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity
                .badRequest()
                .body("Secret count should be positive. Can't accept value -1"));
        verify(appRequesterRuntimeSecretService, times(0))
                .setAppRuntimeSecretCount(APP_ADDRESS, secretCount);
    }
    // endregion
}