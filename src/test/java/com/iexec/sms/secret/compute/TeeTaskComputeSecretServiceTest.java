package com.iexec.sms.secret.compute;

import com.iexec.sms.encryption.EncryptionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TeeTaskComputeSecretServiceTest {
    private static final String APP_ADDRESS = "appAddress";
    private static final String DECRYPTED_SECRET_VALUE = "I'm a secret.";
    private static final String ENCRYPTED_SECRET_VALUE = "I'm an encrypted secret.";
    private static final String METRICS_PREFIX = "iexec.sms.secrets.teeTaskCompute.";
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

    @InjectMocks
    @Spy
    TeeTaskComputeSecretService teeTaskComputeSecretService;

    @Captor
    ArgumentCaptor<TeeTaskComputeSecret> computeSecretCaptor;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void beforeEach() {
        meterRegistry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(meterRegistry);

        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void afterEach() {
        meterRegistry.clear();
        Metrics.globalRegistry.clear();
    }

    // region init
    @Test
    void shouldRegisterCounter() {
        final long initialCount = 5L;
        when(teeTaskComputeSecretRepository.count()).thenReturn(initialCount);

        teeTaskComputeSecretService.init();

        assertInitialCount(initialCount);
    }
    // endregion

    // region encryptAndSaveSecret
    @Test
    void shouldAddSecret() {
        doReturn(false).when(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");
        when(encryptionService.encrypt(DECRYPTED_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);

        teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0", DECRYPTED_SECRET_VALUE);

        verify(teeTaskComputeSecretRepository, times(1)).save(computeSecretCaptor.capture());
        final TeeTaskComputeSecret savedTeeTaskComputeSecret = computeSecretCaptor.getValue();
        Assertions.assertThat(savedTeeTaskComputeSecret.getHeader().getKey()).isEqualTo("0");
        Assertions.assertThat(savedTeeTaskComputeSecret.getHeader().getOnChainObjectAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(savedTeeTaskComputeSecret.getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
        assertAddedCount(1);
    }

    @Test
    void shouldNotAddSecretSinceAlreadyExist() {
        doReturn(true).when(teeTaskComputeSecretService)
                .isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        teeTaskComputeSecretService.encryptAndSaveSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0", DECRYPTED_SECRET_VALUE);

        assertAddedCount(0);
        verify(teeTaskComputeSecretRepository, times(0)).save(computeSecretCaptor.capture());
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
    void secretShouldExist() {
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0"))
                .thenReturn(Optional.of(COMPUTE_SECRET));

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        Assertions.assertThat(isSecretPresent).isTrue();
    }

    @Test
    void secretShouldNotExist() {
        when(teeTaskComputeSecretService.getSecret(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0"))
                .thenReturn(Optional.empty());

        final boolean isSecretPresent = teeTaskComputeSecretService.isSecretPresent(OnChainObjectType.APPLICATION, APP_ADDRESS, SecretOwnerRole.APPLICATION_DEVELOPER, "", "0");

        Assertions.assertThat(isSecretPresent).isFalse();
    }
    // endregion

    // region stored count
    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 10, Long.MAX_VALUE})
    void storedCount(long expectedCount) {
        when(teeTaskComputeSecretRepository.count()).thenReturn(expectedCount);

        assertStoredCount(expectedCount);

        verify(teeTaskComputeSecretRepository).count();
    }
    // endregion

    // region utils
    void assertInitialCount(long expectedCount) {
        final Counter initialCounter = meterRegistry.find(METRICS_PREFIX + "initial")
                .counter();

        assertThat(initialCounter)
                .extracting(Counter::count)
                .isEqualTo((double) expectedCount);
    }

    void assertAddedCount(long expectedCount) {
        final Counter addedCounter = meterRegistry.find(METRICS_PREFIX + "added")
                .counter();

        assertThat(addedCounter)
                .extracting(Counter::count)
                .isEqualTo((double) expectedCount);
    }

    void assertStoredCount(long expectedCount) {
        final Gauge storedGauge = meterRegistry.find(METRICS_PREFIX + "stored")
                .gauge();

        assertThat(storedGauge)
                .extracting(Gauge::value)
                .isEqualTo((double) expectedCount);
    }
    // endregion
}