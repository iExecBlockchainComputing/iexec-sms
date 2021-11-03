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

import java.util.Optional;

import static org.mockito.Mockito.*;

class ApplicationRuntimeSecretControllerTest {
    private static final String AUTHORIZATION = "authorization";
    private static final String APP_ADDRESS = "appAddress";
    private static final String SECRET_VALUE = "I'm a secret.";
    private static final String CHALLENGE = "challenge";

    @Mock
    ApplicationRuntimeSecretService applicationRuntimeSecretService;

    @Mock
    AuthorizationService authorizationService;

    @InjectMocks
    ApplicationRuntimeSecretController applicationRuntimeSecretController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldAddSecret() {
        long secretIndex = 0;
        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(applicationRuntimeSecretService.getSecret(APP_ADDRESS, secretIndex))
                .thenReturn(Optional.empty());
        doNothing().when(applicationRuntimeSecretService).encryptAndSaveSecret(APP_ADDRESS, secretIndex, SECRET_VALUE);

        ResponseEntity<String> result = applicationRuntimeSecretController.addApplicationRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                SECRET_VALUE
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.noContent().build());
    }

    @Test
    void shouldNotAddSecretSinceNotSignedByOwner() {
        long secretIndex = 0;
        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(false);


        ResponseEntity<String> result = applicationRuntimeSecretController.addApplicationRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                SECRET_VALUE
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        verify(applicationRuntimeSecretService, times(0))
                .getSecret(APP_ADDRESS, secretIndex);
        verify(applicationRuntimeSecretService, times(0))
                .encryptAndSaveSecret(APP_ADDRESS, secretIndex, SECRET_VALUE);
    }

    @Test
    void shouldNotAddSecretSinceSecretAlreadyExists() {
        long secretIndex = 0;
        when(authorizationService.getChallengeForSetAppRuntimeSecret(APP_ADDRESS, secretIndex, SECRET_VALUE))
                .thenReturn(CHALLENGE);
        when(authorizationService.isSignedByOwner(CHALLENGE, AUTHORIZATION, APP_ADDRESS))
                .thenReturn(true);
        when(applicationRuntimeSecretService.getSecret(APP_ADDRESS, secretIndex))
                .thenReturn(Optional.of(new ApplicationRuntimeSecret(APP_ADDRESS, secretIndex, SECRET_VALUE)));

        ResponseEntity<String> result = applicationRuntimeSecretController.addApplicationRuntimeSecret(
                AUTHORIZATION,
                APP_ADDRESS,
                SECRET_VALUE
        );

        Assertions.assertThat(result).isEqualTo(ResponseEntity.status(HttpStatus.CONFLICT).build());

        verify(applicationRuntimeSecretService, times(0))
                .encryptAndSaveSecret(APP_ADDRESS, secretIndex, SECRET_VALUE);
    }
}