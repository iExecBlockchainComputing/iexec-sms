package com.iexec.sms.secret.compute;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.CacheSecretService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
            .fixedSecretOwner("")
            .key("0")
            .value(ENCRYPTED_SECRET_VALUE)
            .build();

    @Mock
    TeeTaskComputeSecretRepository teeTaskComputeSecretRepository;

    @Mock
    EncryptionService encryptionService;

    @Mock
    MeasuredSecretService measuredSecretService;

    @Mock
    CacheSecretService<TeeTaskComputeSecretHeader> teeTaskComputeCacheSecretService;

    @InjectMocks
    @Spy
    TeeTaskComputeSecretService teeTaskComputeSecretService;

    @Captor
    ArgumentCaptor<TeeTaskComputeSecret> computeSecretCaptor;


    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    // region encryptAndSaveSecret
    @Test
    void shouldAddSecret() {
        doReturn(false).when(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        when(encryptionService.encrypt(DECRYPTED_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);
        when(teeTaskComputeSecretRepository.save(any())).thenReturn(new TeeTaskComputeSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0", ENCRYPTED_SECRET_VALUE));
        final boolean secretAdded = teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0", DECRYPTED_SECRET_VALUE);

        assertAll(
                () -> assertTrue(secretAdded),
                () -> verify(teeTaskComputeSecretRepository, times(1)).save(computeSecretCaptor.capture())
        );

        final TeeTaskComputeSecret savedTeeTaskComputeSecret = computeSecretCaptor.getValue();
        assertAll(
                () -> assertEquals("0", savedTeeTaskComputeSecret.getHeader().getKey()),
                () -> assertEquals(savedTeeTaskComputeSecret.getHeader().getOnChainObjectAddress(), APP_ADDRESS.toLowerCase()),
                () -> assertEquals(ENCRYPTED_SECRET_VALUE, savedTeeTaskComputeSecret.getValue()),
                () -> verify(measuredSecretService).newlyAddedSecret()
        );
    }

    @Test
    void shouldNotAddSecretSinceAlreadyExist() {
        doReturn(true).when(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0", DECRYPTED_SECRET_VALUE);

        verify(teeTaskComputeSecretRepository, times(0)).save(computeSecretCaptor.capture());
        verify(measuredSecretService, times(0)).newlyAddedSecret();
    }
    // endregion

    // region getSecret
    @Test
    void shouldGetSecret() {
        when(teeTaskComputeSecretRepository.findById(any()))
                .thenReturn(Optional.of(COMPUTE_SECRET));
        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(DECRYPTED_SECRET_VALUE);

        Optional<TeeTaskComputeSecret> decryptedSecret = teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        Assertions.assertThat(decryptedSecret).isPresent();
        Assertions.assertThat(decryptedSecret.get().getHeader().getKey()).isEqualTo("0");
        Assertions.assertThat(decryptedSecret.get().getHeader().getOnChainObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(decryptedSecret.get().getValue()).isEqualTo(DECRYPTED_SECRET_VALUE);
        verify(encryptionService, Mockito.times(1)).decrypt(any());
    }
    // endregion

    // region isSecretPresent
    @Test
    void shouldGetSecretExistFromDBAndPutInCache() {
        when(teeTaskComputeSecretRepository.findById(any(TeeTaskComputeSecretHeader.class)))
                .thenReturn(Optional.of(COMPUTE_SECRET));
        when(teeTaskComputeCacheSecretService.lookSecretExistenceInCache(any(TeeTaskComputeSecretHeader.class))).thenReturn(null);

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertTrue(isSecretPresent)
        );
    }

    @Test
    void shouldGetSecretExistFromCache() {
        when(teeTaskComputeCacheSecretService.lookSecretExistenceInCache(any(TeeTaskComputeSecretHeader.class))).thenReturn(true);
        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertTrue(isSecretPresent),
                () -> verify(teeTaskComputeSecretRepository, times(0)).findById(any(TeeTaskComputeSecretHeader.class))
        );
    }

    @Test
    void shouldGetSecretNotExistFromCache() {
        when(teeTaskComputeCacheSecretService.lookSecretExistenceInCache(any(TeeTaskComputeSecretHeader.class))).thenReturn(false);
        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertFalse(isSecretPresent),
                () -> verify(teeTaskComputeSecretRepository, times(0)).findById(any(TeeTaskComputeSecretHeader.class))
        );
    }

    @Test
    void secretShouldNotExist() {
        when(teeTaskComputeCacheSecretService.lookSecretExistenceInCache(any(TeeTaskComputeSecretHeader.class))).thenReturn(null);
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0"))
                .thenReturn(Optional.empty());

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        Assertions.assertThat(isSecretPresent).isFalse();
    }
    // endregion
}
