package com.iexec.sms.tee;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.iexec.common.tee.TeeEnclaveProvider.GRAMINE;
import static com.iexec.common.tee.TeeEnclaveProvider.SCONE;
import static org.junit.jupiter.api.Assertions.*;

class TeeEnclaveProviderConverterTests {
    private final TeeEnclaveProviderConverter converter = new TeeEnclaveProviderConverter();

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
