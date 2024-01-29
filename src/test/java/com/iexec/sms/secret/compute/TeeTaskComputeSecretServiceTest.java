package com.iexec.sms.secret.compute;

import com.iexec.sms.encryption.EncryptionService;
import com.iexec.sms.secret.MeasuredSecretService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
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

        final boolean secretAdded = teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0", DECRYPTED_SECRET_VALUE);

        Assertions.assertThat(secretAdded).isTrue();
        verify(teeTaskComputeSecretRepository, times(1)).save(computeSecretCaptor.capture());

        final TeeTaskComputeSecret savedTeeTaskComputeSecret = computeSecretCaptor.getValue();
        Assertions.assertThat(savedTeeTaskComputeSecret.getHeader().getKey()).isEqualTo("0");
        Assertions.assertThat(savedTeeTaskComputeSecret.getHeader().getOnChainObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(savedTeeTaskComputeSecret.getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
        verify(measuredSecretService).newlyAddedSecret();
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
    void shouldGetSecretExistenceFromDBAndPutInCache(CapturedOutput output) {
        when(teeTaskComputeSecretRepository.findById(any(TeeTaskComputeSecretHeader.class)))
                .thenReturn(Optional.of(COMPUTE_SECRET));

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        assertAll(
                () -> assertTrue(isSecretPresent),
                () -> assertThat(output.getOut()).contains("Search secret existence in cache"),
                () -> assertThat(output.getOut()).contains("Secret existence was not found in cache"),
                () -> assertThat(output.getOut()).doesNotContain("Secret existence was found in cache"),
                () -> assertThat(output.getOut()).contains("Put secret existence in cache")
        );
    }

    @Test
    void shouldGetSecretExistenceFromCache(CapturedOutput output) {
        when(teeTaskComputeSecretRepository.findById(any(TeeTaskComputeSecretHeader.class)))
                .thenReturn(Optional.of(COMPUTE_SECRET));

        teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        final int logLengthForFirstCall = output.getOut().length();
        final boolean resultSecondCall = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        final String secondCallLogs = output.getOut().substring(logLengthForFirstCall - 1);
        assertAll(
                () -> assertTrue(resultSecondCall),
                //put 1 bellow means no new invocation since 1st call
                () -> verify(teeTaskComputeSecretRepository, times(1)).findById(any(TeeTaskComputeSecretHeader.class)),
                () -> assertThat(secondCallLogs).doesNotContain("Secret existence was not found in cache"),
                () -> assertThat(secondCallLogs).doesNotContain("Put secret existence in cache"),
                () -> assertThat(secondCallLogs).contains("Search secret existence in cache"),
                () -> assertThat(secondCallLogs).contains("Secret existence was found in cache")
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
