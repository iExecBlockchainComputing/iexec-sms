package com.iexec.sms.secret.compute;

import com.iexec.sms.encryption.EncryptionService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.mockito.Mockito.*;

class TeeTaskComputeSecretServiceTest {
    private static final String APP_ADDRESS = "appAddress";
    private static final String DECRYPTED_SECRET_VALUE = "I'm a secret.";
    private static final String ENCRYPTED_SECRET_VALUE = "I'm an encrypted secret.";
    private static final TeeTaskComputeSecret COMPUTE_SECRET = TeeTaskComputeSecret
            .builder()
            .onChainObjectType(OnChainObjectType.APPLICATION)
            .onChainObjectAddress(APP_ADDRESS.toLowerCase())
            .secretOwnerRole(SecretOwnerRole.APPLICATION_DEVELOPER)
            .index(0)
            .value(ENCRYPTED_SECRET_VALUE)
            .build();

    @Mock
    TeeTaskComputeSecretRepository teeTaskComputeSecretRepository;

    @Mock
    EncryptionService encryptionService;

    @InjectMocks
    @Spy
    TeeTaskComputeSecretService teeTaskComputeSecretService;

    @Captor
    ArgumentCaptor<TeeTaskComputeSecret> computeSecretCaptor;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region encryptAndSaveSecret
    @Test
    void shouldAddSecret() {
        doReturn(false).when(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0);
        when(encryptionService.encrypt(DECRYPTED_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);

        teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0, DECRYPTED_SECRET_VALUE);

        verify(teeTaskComputeSecretRepository, times(1)).save(computeSecretCaptor.capture());
        final TeeTaskComputeSecret savedTeeTaskComputeSecret = computeSecretCaptor.getValue();
        Assertions.assertThat(savedTeeTaskComputeSecret.getIndex()).isZero();
        Assertions.assertThat(savedTeeTaskComputeSecret.getOnChainObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(savedTeeTaskComputeSecret.getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
    }

    @Test
    void shouldNotAddSecretSinceAlreadyExist() {
        doReturn(true).when(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0);

        teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0, DECRYPTED_SECRET_VALUE);

        verify(teeTaskComputeSecretRepository, times(0)).save(computeSecretCaptor.capture());
    }
    // endregion

    // region getSecret
    @Test
    void shouldGetSecret() {
        when(teeTaskComputeSecretRepository.findOne(any()))
                .thenReturn(Optional.of(COMPUTE_SECRET));
        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(DECRYPTED_SECRET_VALUE);

        // First call will not decrypt secret value
        Optional<TeeTaskComputeSecret> encryptedSecret = teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0, false);
        Assertions.assertThat(encryptedSecret).isPresent();
        Assertions.assertThat(encryptedSecret.get().getIndex()).isZero();
        Assertions.assertThat(encryptedSecret.get().getOnChainObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(encryptedSecret.get().getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
        verify(encryptionService, Mockito.times(0)).decrypt(any());

        // Second call will decrypt secret value
        Optional<TeeTaskComputeSecret> decryptedSecret = teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0, true);
        Assertions.assertThat(decryptedSecret).isPresent();
        Assertions.assertThat(decryptedSecret.get().getIndex()).isZero();
        Assertions.assertThat(decryptedSecret.get().getOnChainObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(decryptedSecret.get().getValue()).isEqualTo(DECRYPTED_SECRET_VALUE);
        verify(encryptionService, Mockito.times(1)).decrypt(any());
    }
    // endregion

    // region isSecretPresent
    @Test
    void secretShouldExist() {
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0, false))
                .thenReturn(Optional.of(COMPUTE_SECRET));

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0);

        Assertions.assertThat(isSecretPresent).isTrue();
    }

    @Test
    void secretShouldNotExist() {
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0, false))
                .thenReturn(Optional.empty());

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, null, 0);

        Assertions.assertThat(isSecretPresent).isFalse();
    }
    // endregion
}