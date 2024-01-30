package com.iexec.sms.secret.compute;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.sms.MemoryLogAppender;
import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.LoggerFactory;

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

    @InjectMocks
    @Spy
    TeeTaskComputeSecretService teeTaskComputeSecretService;

    @Captor
    ArgumentCaptor<TeeTaskComputeSecret> computeSecretCaptor;

    private static MemoryLogAppender memoryLogAppender;

    @BeforeAll
    static void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.iexec.sms.secret");
        memoryLogAppender = new MemoryLogAppender();
        memoryLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryLogAppender);
        memoryLogAppender.start();
    }

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        memoryLogAppender.reset();
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
                () -> verify(teeTaskComputeSecretRepository, times(1)).save(computeSecretCaptor.capture()),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
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

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertTrue(isSecretPresent),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was not found in cache")),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache"))
        );
    }

    @Test
    void shouldGetSecretExistFromCache() {
        when(teeTaskComputeSecretRepository.findById(any(TeeTaskComputeSecretHeader.class)))
                .thenReturn(Optional.of(COMPUTE_SECRET));

        teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        memoryLogAppender.reset();
        final boolean resultSecondCall = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertTrue(resultSecondCall),
                //put 1 bellow means no new invocation since 1st call
                () -> verify(teeTaskComputeSecretRepository, times(1)).findById(any(TeeTaskComputeSecretHeader.class)),
                () -> assertTrue(memoryLogAppender.doesNotContains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache")),
                () -> assertTrue(memoryLogAppender.contains("exist:true"))
        );
    }

    @Test
    void shouldGetSecretNotExistFromCache() {
        when(teeTaskComputeSecretRepository.findById(any(TeeTaskComputeSecretHeader.class)))
                .thenReturn(Optional.empty());

        teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        memoryLogAppender.reset();
        final boolean resultSecondCall = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertFalse(resultSecondCall),
                //put 1 bellow means no new invocation since 1st call
                () -> verify(teeTaskComputeSecretRepository, times(1)).findById(any(TeeTaskComputeSecretHeader.class)),
                () -> assertTrue(memoryLogAppender.doesNotContains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Search secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache")),
                () -> assertTrue(memoryLogAppender.contains("exist:false"))
        );
    }

    @Test
    void secretShouldNotExist() {
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0"))
                .thenReturn(Optional.empty());

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        Assertions.assertThat(isSecretPresent).isFalse();
    }
    // endregion
}
