package com.iexec.sms.secret.app;

import com.iexec.sms.encryption.EncryptionService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.mockito.Mockito.*;

class TeeTaskRuntimeSecretServiceTest {
    private static final String APP_ADDRESS = "appAddress";
    private static final String DECRYPTED_SECRET_VALUE = "I'm a secret.";
    private static final String ENCRYPTED_SECRET_VALUE = "I'm an encrypted secret.";
    private static final TeeTaskRuntimeSecret RUNTIME_SECRET = new TeeTaskRuntimeSecret(
            DeployedObjectType.APP,
            APP_ADDRESS.toLowerCase(),
            OwnerRole.APP_DEVELOPER,
            null,
            0,
            ENCRYPTED_SECRET_VALUE);

    @Mock
    TeeTaskRuntimeSecretRepository teeTaskRuntimeSecretRepository;

    @Mock
    EncryptionService encryptionService;

    @InjectMocks
    @Spy
    TeeTaskRuntimeSecretService teeTaskRuntimeSecretService;

    @Captor
    ArgumentCaptor<TeeTaskRuntimeSecret> runtimeSecretCaptor;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region encryptAndSaveSecret
    @Test
    void shouldAddSecret() {
        doReturn(false).when(teeTaskRuntimeSecretService)
                .isSecretPresent(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0);
        when(encryptionService.encrypt(DECRYPTED_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);

        teeTaskRuntimeSecretService.encryptAndSaveSecret(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0, DECRYPTED_SECRET_VALUE);

        verify(teeTaskRuntimeSecretRepository, times(1)).save(runtimeSecretCaptor.capture());
        final TeeTaskRuntimeSecret savedTeeTaskRuntimeSecret = runtimeSecretCaptor.getValue();
        Assertions.assertThat(savedTeeTaskRuntimeSecret.getIndex()).isZero();
        Assertions.assertThat(savedTeeTaskRuntimeSecret.getDeployedObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(savedTeeTaskRuntimeSecret.getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
    }

    @Test
    void shouldNotAddSecretSinceAlreadyExist() {
        doReturn(true).when(teeTaskRuntimeSecretService)
                .isSecretPresent(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0);

        teeTaskRuntimeSecretService.encryptAndSaveSecret(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0, DECRYPTED_SECRET_VALUE);

        verify(teeTaskRuntimeSecretRepository, times(0)).save(runtimeSecretCaptor.capture());
    }
    // endregion

    // region getSecret
    @Test
    void shouldGetSecret() {
        when(teeTaskRuntimeSecretRepository.findOne(any()))
                .thenReturn(Optional.of(RUNTIME_SECRET));
        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(DECRYPTED_SECRET_VALUE);

        // First call will not decrypt secret value
        Optional<TeeTaskRuntimeSecret> encryptedSecret = teeTaskRuntimeSecretService.getSecret(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0, false);
        Assertions.assertThat(encryptedSecret).isPresent();
        Assertions.assertThat(encryptedSecret.get().getIndex()).isZero();
        Assertions.assertThat(encryptedSecret.get().getDeployedObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(encryptedSecret.get().getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
        verify(encryptionService, Mockito.times(0)).decrypt(any());

        // Second call will decrypt secret value
        Optional<TeeTaskRuntimeSecret> decryptedSecret = teeTaskRuntimeSecretService.getSecret(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0, true);
        Assertions.assertThat(decryptedSecret).isPresent();
        Assertions.assertThat(decryptedSecret.get().getIndex()).isZero();
        Assertions.assertThat(decryptedSecret.get().getDeployedObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(decryptedSecret.get().getValue()).isEqualTo(DECRYPTED_SECRET_VALUE);
        verify(encryptionService, Mockito.times(1)).decrypt(any());
    }
    // endregion

    // region isSecretPresent
    @Test
    void secretShouldExist() {
        when(teeTaskRuntimeSecretService.getSecret(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0, false))
                .thenReturn(Optional.of(RUNTIME_SECRET));

        final boolean isSecretPresent = teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0);

        Assertions.assertThat(isSecretPresent).isTrue();
    }

    @Test
    void secretShouldNotExist() {
        when(teeTaskRuntimeSecretService.getSecret(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0, false))
                .thenReturn(Optional.empty());

        final boolean isSecretPresent = teeTaskRuntimeSecretService.isSecretPresent(DeployedObjectType.APP, APP_ADDRESS, OwnerRole.APP_DEVELOPER, null, 0);

        Assertions.assertThat(isSecretPresent).isFalse();
    }
    // endregion
}