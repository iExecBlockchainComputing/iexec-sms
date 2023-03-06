package com.iexec.sms.tee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.iexec.common.tee.TeeFramework.GRAMINE;
import static com.iexec.common.tee.TeeFramework.SCONE;
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
