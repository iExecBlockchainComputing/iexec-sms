package com.iexec.sms.secret;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iexec.sms.MemoryLogAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class AbstractSecretServiceTests {

    private final static String KEY = "KEY";

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


    @Test
    void shouldSucceed() {
        AbstractSecretServiceTU secretServiceTU = new AbstractSecretServiceTU();
        secretServiceTU.putSecretExistenceInCache(KEY);
        boolean found = secretServiceTU.lookSecretExistenceInCache(KEY);
        assertAll(
                () -> assertTrue(found),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache"))
        );
    }

    @Test
    void shouldFailedToPutInCacheWhenKeyIsNull() {
        AbstractSecretServiceTU secretServiceTU = new AbstractSecretServiceTU();
        secretServiceTU.putSecretExistenceInCache(null);
        assertTrue(memoryLogAppender.contains("Key is NULL, unable to use cache"));
    }

    @Test
    void shouldFailedToReadFromCacheWhenKeyIsNull() {
        AbstractSecretServiceTU secretServiceTU = new AbstractSecretServiceTU();
        boolean found = secretServiceTU.lookSecretExistenceInCache(null);
        assertAll(
                () -> assertFalse(found),
                () -> assertTrue(memoryLogAppender.contains("Key is NULL, unable to use cache"))
        );
    }

    @Test
    void shouldFailedToReadFromCacheWhenKeyIsNotInCache() {
        AbstractSecretServiceTU secretServiceTU = new AbstractSecretServiceTU();
        boolean found = secretServiceTU.lookSecretExistenceInCache("MISSING");
        assertAll(
                () -> assertFalse(found),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was not found in cache"))
        );
    }

    private static class AbstractSecretServiceTU extends AbstractSecretService<String> {
    }
}
