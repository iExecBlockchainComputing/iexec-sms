/*
 * Copyright 2022-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.tee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.iexec.commons.poco.tee.TeeFramework.GRAMINE;
import static com.iexec.commons.poco.tee.TeeFramework.SCONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeeFrameworkConverterTests {
    private final TeeFrameworkConverter converter = new TeeFrameworkConverter();

    // region convert
    @Test
    void shouldConvertScone() {
        assertEquals(SCONE, converter.convert("scone"));
        assertEquals(SCONE, converter.convert("Scone"));
        assertEquals(SCONE, converter.convert("SCONE"));
        assertEquals(SCONE, converter.convert("sCoNe"));
    }

    @Test
    void shouldConvertGramine() {
        assertEquals(GRAMINE, converter.convert("gramine"));
        assertEquals(GRAMINE, converter.convert("Gramine"));
        assertEquals(GRAMINE, converter.convert("GRAMINE"));
        assertEquals(GRAMINE, converter.convert("gRaMiNe"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "azer"})
    void shouldNotConvertBadValue(String value) {
        assertThrows(IllegalArgumentException.class, () -> converter.convert(value));
    }
    // endregion
}
