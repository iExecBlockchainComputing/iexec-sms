package com.iexec.sms.tee.session.scone;

import com.iexec.common.tee.TeeEnclaveProvider;
import com.iexec.sms.tee.session.scone.cas.CasClient;
import com.iexec.sms.tee.session.scone.palaemon.PalaemonSessionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SconeStackTests {
    @Test
    void shouldCreateSconeStack() {
        final SconeStack stack =
                new SconeStack(mock(PalaemonSessionService.class), mock(CasClient.class));
        assertEquals(TeeEnclaveProvider.SCONE, stack.getTeeEnclaveProvider());
    }
}