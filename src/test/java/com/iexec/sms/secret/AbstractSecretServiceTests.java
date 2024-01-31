/*
 * Copyright 2024-2024 IEXEC BLOCKCHAIN TECH
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
    void shouldSucceedWithTrueValue() {
        AbstractSecretServiceTU secretServiceTU = new AbstractSecretServiceTU();
        secretServiceTU.putSecretExistenceInCache(KEY, true);
        boolean found = secretServiceTU.lookSecretExistenceInCache(KEY);
        assertAll(
                () -> assertTrue(found),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache[key:KEY, exist:true]"))
        );
    }

    @Test
    void shouldSucceedWithFalseValue() {
        AbstractSecretServiceTU secretServiceTU = new AbstractSecretServiceTU();
        secretServiceTU.putSecretExistenceInCache(KEY, false);
        boolean found = secretServiceTU.lookSecretExistenceInCache(KEY);
        assertAll(
                () -> assertFalse(found),
                () -> assertTrue(memoryLogAppender.contains("Put secret existence in cache")),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was found in cache[key:KEY, exist:false]"))
        );
    }


    @Test
    void shouldFailedToPutInCacheWhenKeyIsNull() {
        AbstractSecretServiceTU secretServiceTU = new AbstractSecretServiceTU();
        secretServiceTU.putSecretExistenceInCache(null, true);
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
        Boolean found = secretServiceTU.lookSecretExistenceInCache("MISSING");
        assertAll(
                () -> assertNull(found),
                () -> assertTrue(memoryLogAppender.contains("Secret existence was not found in cache"))
        );
    }

    private static class AbstractSecretServiceTU extends AbstractSecretService<String> {
    }
}
