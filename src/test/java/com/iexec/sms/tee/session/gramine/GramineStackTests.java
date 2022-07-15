package com.iexec.sms.tee.session.gramine;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.tee.session.gramine.sps.SpsClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GramineStackTests {
    @Test
    void shouldCreateGramineStack() {
        final GramineStack stack =
                new GramineStack(mock(GramineSessionService.class), mock(SpsClient.class));
        assertEquals(TeeEnclaveProvider.GRAMINE, stack.getTeeEnclaveProvider());
    }

}