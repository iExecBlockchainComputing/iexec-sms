/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.sms.encryption;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.sms.MemoryLogAppender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EncryptionServiceTests {

    public static final String AES_KEY_FILE = "/aes.key";
    @TempDir
    public File tempDir;
    private String aesKeyPath;
    private EncryptionService service;
    private static MemoryLogAppender memoryLogAppender;

    @BeforeAll
    static void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.iexec.sms.encryption");
        memoryLogAppender = new MemoryLogAppender();
        memoryLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryLogAppender);
        memoryLogAppender.start();
    }

    @BeforeEach
    void beforeEach() {
        memoryLogAppender.reset();
        aesKeyPath = tempDir.getAbsolutePath() + AES_KEY_FILE;
        service = new EncryptionService(new EncryptionConfiguration(aesKeyPath));
    }

    // region CreateAesKey
    @Test
    void shouldCreateAesKey() {
        final String data = "data mock";
        service.checkAlgoAndPermissions();

        final File aesKeyFile = new File(aesKeyPath);
        assertAll(
                () -> assertThat(aesKeyFile).exists(),
                () -> assertThat(aesKeyFile).canRead(),
                () -> assertThat(aesKeyFile.canExecute()).isFalse(),
                () -> assertThat(aesKeyFile.canWrite()).isFalse(),
                () -> assertThat(service.decrypt(service.encrypt(data))).isEqualTo(data),
                () -> assertThat(memoryLogAppender.contains("AES key file set to readOnly")).isTrue(),
                () -> assertThat(memoryLogAppender.contains("success:true")).isTrue()
        );
        //second call, no need to set permissions again
        memoryLogAppender.reset();
        service.checkAlgoAndPermissions();
        assertThat(memoryLogAppender.doesNotContains("AES key file set to readOnly")).isTrue();
    }
    // endregion

    //region ExceptionInInitializerError
    @Test
    void shouldReturnExceptionInInitializerErrorWhenCheckOnPostConstructFailed() {
        EncryptionService spyEncryptionService = Mockito.spy(service);
        when(spyEncryptionService.decrypt(any())).thenReturn("bad message");

        assertThatExceptionOfType(ExceptionInInitializerError.class)
                .isThrownBy(spyEncryptionService::checkAlgoAndPermissions);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldReturnExceptionInInitializerErrorWhenAesKeyPathIsNullOrEmpty(String aesKeyPath) {
        final EncryptionConfiguration encryptionConfiguration = new EncryptionConfiguration(aesKeyPath);
        assertThatExceptionOfType(ExceptionInInitializerError.class)
                .isThrownBy(() -> new EncryptionService(encryptionConfiguration));
    }

    @Test
    void shouldReturnExceptionInInitializerErrorWhenAesKeyFileIsEmpty() {
        final String aesKeyPath = tempDir.getAbsolutePath() + "/aes2.key";
        final File aesKeyFile = new File(aesKeyPath);
        Assertions.assertThatCode(aesKeyFile::createNewFile).doesNotThrowAnyException();

        final EncryptionConfiguration encryptionConfiguration = new EncryptionConfiguration(aesKeyPath);
        assertThatExceptionOfType(ExceptionInInitializerError.class)
                .isThrownBy(() -> new EncryptionService(encryptionConfiguration));
    }

    @Test
    void shouldReturnExceptionInInitializerErrorWhenFailedToCreateAesKeyFile() {
        final String aesKeyPath = tempDir.getAbsolutePath() + "/aes2.key";
        assertThat(tempDir.setWritable(false)).isTrue();
        final EncryptionConfiguration encryptionConfiguration = new EncryptionConfiguration(aesKeyPath);
        assertThatExceptionOfType(ExceptionInInitializerError.class)
                .isThrownBy(() -> new EncryptionService(encryptionConfiguration));
    }

    @Test
    void shouldReturnExceptionInInitializerErrorWhenFailedToSetPermissions() {
        EncryptionService spyEncryptionService = Mockito.spy(service);
        when(spyEncryptionService.checkOrFixReadOnlyPermissions(aesKeyPath)).thenReturn(false);
        assertThatExceptionOfType(ExceptionInInitializerError.class)
                .isThrownBy(spyEncryptionService::checkAlgoAndPermissions);
    }
    // endregion

    // region Encrypt
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void shouldReturnEmptyOnEncryptIfBadInput(String input) {
        assertThat(service.decrypt(service.encrypt(input))).isEmpty();
    }


    @Test
    void shouldReturnEmptyIfErrorOccurredAesEncrypt() {
        ReflectionTestUtils.setField(service, "aesKey", "badKey".getBytes());
        assertThat(service.decrypt(service.encrypt("test"))).isEmpty();
    }
    // endregion

    // region Decrypt
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "0x"})
    void shouldReturnEmptyIfFailedToDecryptOrBadInputData(String input) {
        assertThat(service.decrypt(service.decrypt(input))).isEmpty();
    }
    // endregion
}
