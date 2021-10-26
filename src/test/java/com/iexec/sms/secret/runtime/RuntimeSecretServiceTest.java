package com.iexec.sms.secret.runtime;

import com.iexec.sms.encryption.EncryptionService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.mockito.Mockito.*;

class RuntimeSecretServiceTest {
    private static final String APP_ADDRESS = "appAddress";
    private static final String DECRYPTED_SECRET_VALUE = "I'm a secret.";
    private static final String ENCRYPTED_SECRET_VALUE = "I'm an encrypted secret.";
    private static final RuntimeSecret RUNTIME_SECRET = new RuntimeSecret(APP_ADDRESS.toLowerCase(), 0, ENCRYPTED_SECRET_VALUE);
    static {
        RUNTIME_SECRET.setValue(ENCRYPTED_SECRET_VALUE, true);  // Just set `isEncryptedValue` to `true`
    }

    @Mock
    RuntimeSecretRepository runtimeSecretRepository;

    @Mock
    EncryptionService encryptionService;

    @InjectMocks
    @Spy
    RuntimeSecretService runtimeSecretService;

    @Captor
    ArgumentCaptor<RuntimeSecret> runtimeSecretCaptor;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldAddSecret() {
        when(encryptionService.encrypt(DECRYPTED_SECRET_VALUE))
                .thenReturn(ENCRYPTED_SECRET_VALUE);

        runtimeSecretService.addSecret(APP_ADDRESS, 0, DECRYPTED_SECRET_VALUE);

        verify(runtimeSecretRepository, times(1)).save(runtimeSecretCaptor.capture());
        final RuntimeSecret savedRuntimeSecret = runtimeSecretCaptor.getValue();
        Assertions.assertThat(savedRuntimeSecret.getIndex()).isZero();
        Assertions.assertThat(savedRuntimeSecret.getAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(savedRuntimeSecret.getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);
    }

    @Test
    void shouldGetSecret() {
        when(runtimeSecretRepository.findRuntimeSecretByAppAddressAndIndex(APP_ADDRESS.toLowerCase(), 0))
                .thenReturn(Optional.of(RUNTIME_SECRET));
        when(encryptionService.decrypt(ENCRYPTED_SECRET_VALUE))
                .thenReturn(DECRYPTED_SECRET_VALUE);

        // First call will not decrypt secret value
        Optional<RuntimeSecret> encryptedSecret = runtimeSecretService.getSecret(APP_ADDRESS, 0);
        Assertions.assertThat(encryptedSecret).isPresent();
        Assertions.assertThat(encryptedSecret.get().getIndex()).isZero();
        Assertions.assertThat(encryptedSecret.get().getAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(encryptedSecret.get().getValue()).isEqualTo(ENCRYPTED_SECRET_VALUE);

        // Second call will decrypt secret value
        Optional<RuntimeSecret> decryptedSecret = runtimeSecretService.getSecret(APP_ADDRESS, 0, true);
        Assertions.assertThat(decryptedSecret).isPresent();
        Assertions.assertThat(decryptedSecret.get().getIndex()).isZero();
        Assertions.assertThat(decryptedSecret.get().getAddress()).isEqualTo(APP_ADDRESS.toLowerCase());
        Assertions.assertThat(decryptedSecret.get().getValue()).isEqualTo(DECRYPTED_SECRET_VALUE);
    }
}