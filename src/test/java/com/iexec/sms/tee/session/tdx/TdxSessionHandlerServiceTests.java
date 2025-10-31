/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee.session.tdx;

import com.iexec.sms.api.TeeSessionGenerationError;
import com.iexec.sms.tee.session.generic.TeeSessionGenerationException;
import com.iexec.sms.tee.session.generic.TeeSessionRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TdxSessionHandlerServiceTests {
    private final TdxSessionHandlerService sessionHandlerService = new TdxSessionHandlerService();

    @Test
    void shouldThrow() {
        assertThatThrownBy(() -> sessionHandlerService.buildAndPostSession(TeeSessionRequest.builder().build()))
                .isInstanceOf(TeeSessionGenerationException.class)
                .hasMessage("Not implemented yet")
                .hasFieldOrPropertyWithValue("error", TeeSessionGenerationError.SECURE_SESSION_STORAGE_CALL_FAILED);
    }
}
