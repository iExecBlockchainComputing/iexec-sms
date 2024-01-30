/*
 * Copyright 2024 IEXEC BLOCKCHAIN TECH
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
package com.iexec.sms;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Utility class to capture logs in a list, making it easy to make assertions about messages.
 * Use instead of CapturedOutput
 */
public class MemoryLogAppender extends ListAppender<ILoggingEvent> {

    /**
     * Searches for message in standard logs and exception messages
     *
     * @param msg the message to look for
     * @return true if the message was found and false otherwise.
     */
    public boolean contains(String msg) {
        return this.list.stream()
                .anyMatch(event -> event.toString().contains(msg) ||
                        (event.getThrowableProxy() != null &&
                                event.getThrowableProxy().getMessage().contains(msg)));
    }


    /**
     * Checks that the message does not exist in either standard logs or exception messages
     *
     * @param msg the message to look for
     * @return true if the message was not found and false otherwise.
     */
    public boolean doesNotContains(String string) {
        return !contains(string);
    }

    /**
     * Clean the list
     */
    public void reset() {
        this.list.clear();
    }

    /**
     * Check if the list is empty
     *
     * @return true or false
     */
    public boolean isEmpty() {
        return this.list.isEmpty();
    }


}
