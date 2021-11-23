package com.iexec.sms.secret.app;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.app.owner.AppDeveloperAppRuntimeSecretService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.mockito.Mockito.*;

class AppDeveloperAppRuntimeSecretServiceTest {
    private static final String APP_ADDRESS = "appAddress";
    private static final String DECRYPTED_SECRET_VALUE = "I'm a secret.";
    private static final String ENCRYPTED_SECRET_VALUE = "I'm an encrypted secret.";
    private static final AppRuntimeSecret RUNTIME_SECRET = new AppRuntimeSecret(
            APP_ADDRESS.toLowerCase(),
            0,
            AppRuntimeSecretOwnerRole.APP_DEVELOPER,
            ENCRYPTED_SECRET_VALUE);
    static {
        RUNTIME_SECRET.setValue(ENCRYPTED_SECRET_VALUE, true);  // Just set `isEncryptedValue` to `true`
    }

    @Mock
    AppRuntimeSecretRepository appRuntimeSecretRepository;

    @Mock
    EncryptionService encryptionService;

    @InjectMocks
    @Spy
    AppDeveloperAppRuntimeSecretService appDeveloperAppRuntimeSecretService;

    @Captor
    ArgumentCaptor<AppRuntimeSecret> runtimeSecretCaptor;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldAddSecret() {
        when(encryptionService.encrypt(DECRYPTED_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);

        appDeveloperAppRuntimeSecretService.encryptAndSaveSecret(APP_ADDRESS, 0, DECRYPTED_SECRET_VALUE);

        verify(appRuntimeSecretRepository, times(1)).save(runtimeSecretCaptor.capture());
        final AppRuntimeSecret savedAppRuntimeSecret = runtimeSecretCaptor.getValue();
        Assertions.assertThat(savedAppRuntimeSecret.getIndex()).isZero();
        Assertions.assertThat(savedAppRuntimeSecret.getAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(savedAppRuntimeSecret.getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
    }

    @Test
    void shouldGetSecret() {
        when(appRuntimeSecretRepository.findByAddressAndIndexAndOwnerRole(
                APP_ADDRESS.toLowerCase(),
                0,
                AppRuntimeSecretOwnerRole.APP_DEVELOPER)
        ).thenReturn(Optional.of(RUNTIME_SECRET));
        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(DECRYPTED_SECRET_VALUE);

        // First call will not decrypt secret value
        Optional<AppRuntimeSecret> encryptedSecret = appDeveloperAppRuntimeSecretService.getSecret(APP_ADDRESS, 0);
        Assertions.assertThat(encryptedSecret).isPresent();
        Assertions.assertThat(encryptedSecret.get().getIndex()).isZero();
        Assertions.assertThat(encryptedSecret.get().getAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(encryptedSecret.get().getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
        verify(appDeveloperAppRuntimeSecretService, Mockito.times(0)).decryptSecret(any());

        // Second call will decrypt secret value
        Optional<AppRuntimeSecret> decryptedSecret = appDeveloperAppRuntimeSecretService.getSecret(APP_ADDRESS, 0, true);
        Assertions.assertThat(decryptedSecret).isPresent();
        Assertions.assertThat(decryptedSecret.get().getIndex()).isZero();
        Assertions.assertThat(decryptedSecret.get().getAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(decryptedSecret.get().getValue()).isEqualTo(DECRYPTED_SECRET_VALUE);
        verify(appDeveloperAppRuntimeSecretService, Mockito.times(1)).decryptSecret(any());
    }

    @Test
    void secretShouldExist() {
        when(appDeveloperAppRuntimeSecretService.getSecret(APP_ADDRESS.toLowerCase(), 0))
                .thenReturn(Optional.of(RUNTIME_SECRET));

        Assertions.assertThat(appDeveloperAppRuntimeSecretService.isSecretPresent(APP_ADDRESS, 0))
                .isTrue();
        Mockito.verify(appDeveloperAppRuntimeSecretService).getSecret(APP_ADDRESS, 0);
    }

    @Test
    void secretShouldNotExist() {
        when(appDeveloperAppRuntimeSecretService.getSecret(APP_ADDRESS.toLowerCase(), 0))
                .thenReturn(Optional.empty());

        Assertions.assertThat(appDeveloperAppRuntimeSecretService.isSecretPresent(APP_ADDRESS, 0))
                .isFalse();
        Mockito.verify(appDeveloperAppRuntimeSecretService).getSecret(APP_ADDRESS, 0);
    }
}