package com.iexec.sms.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OutOfServiceRequestFilterTests {

    private MockHttpServletRequest req;
    private MockHttpServletResponse res;
    private MockFilterChain chain;

    @Mock
    private AdminService adminService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        req = new MockHttpServletRequest();
        res = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @Test
    void shouldBeOk() throws Exception {
        Mockito.doReturn(true).when(adminService).isSmsOnline();
        OutOfServiceRequestFilter filter = new OutOfServiceRequestFilter(adminService);

        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void shouldBeOutOfService() throws Exception {
        Mockito.doReturn(false).when(adminService).isSmsOnline();
        OutOfServiceRequestFilter filter = new OutOfServiceRequestFilter(adminService);
        filter.doFilter(req, res, chain);

        assertAll(
                () -> assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
                () -> assertThat(res.getContentAsString()).isEqualTo(OutOfServiceRequestFilter.MAINTENANCE_ERROR)
        );
    }
}
