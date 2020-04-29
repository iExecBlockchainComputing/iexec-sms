package com.iexec.sms.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EncryptionServiceTests {

    @TempDir
    public File tempDir;
  
    @Mock
    private EncryptionConfiguration encryptionConfiguration;

    @InjectMocks
    private EncryptionService encryptionService;

    @Test
    public void shouldCreateAesKey() {
        String data = "data mock";
        // File createdFile = new File(tempDir, "aesKey");
        String aesKeyPath = tempDir.getAbsolutePath() + "aesKey";

        EncryptionService service = new EncryptionService(
                new EncryptionConfiguration(aesKeyPath));

        assertThat(service.decrypt(service.encrypt(data))).isEqualTo(data);
    }
}