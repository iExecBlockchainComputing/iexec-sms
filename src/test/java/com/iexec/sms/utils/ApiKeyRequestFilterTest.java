package com.iexec.sms.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiKeyRequestFilterTest {

    private final String apiKey = "e54fdf4s56df4g";

    @Test
    void shouldPassTheFilterWhenFilterIsActiveAndApiKeyIsCorrect() throws Exception {
        ApiKeyRequestFilter filter = new ApiKeyRequestFilter(apiKey);
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        req.addHeader("X-API-KEY", apiKey);

        filter.doFilter(req,res,chain);

        assertEquals(HttpServletResponse.SC_OK, res.getStatus());
    }

    @Test
    void shouldPassTheFilterWhenFilterIsInactive() throws Exception {
        ApiKeyRequestFilter filter = new ApiKeyRequestFilter(null);
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req,res,chain);

        assertEquals(HttpServletResponse.SC_OK, res.getStatus());
    }

    @Test
    void shouldNotPassTheFilterWhenFilterIsActiveAndApiKeyIsIncorrect() throws Exception {
        ApiKeyRequestFilter filter = new ApiKeyRequestFilter(apiKey);
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req,res,chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
    }
}
