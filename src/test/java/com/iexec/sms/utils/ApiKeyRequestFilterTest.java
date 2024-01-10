/*
 * Copyright 2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiKeyRequestFilterTest {

    private final String apiKey = "e54fdf4s56df4g";
    private MockHttpServletRequest req;
    private MockHttpServletResponse res;
    private MockFilterChain chain;

    @BeforeEach
    public void init() {
        req = new MockHttpServletRequest();
        res = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", apiKey})
    void shouldBeOk(String requestApiKey) throws Exception {
        ApiKeyRequestFilter filter = new ApiKeyRequestFilter(requestApiKey);
        if (null != requestApiKey && !requestApiKey.isBlank()) {
            req.addHeader("X-API-KEY", requestApiKey);
        }
        filter.doFilter(req, res, chain);
        assertEquals(HttpServletResponse.SC_OK, res.getStatus());
    }

    @Test
    void shouldNotPassTheFilterWhenFilterIsActiveAndApiKeyIsNotFilled() throws Exception {
        ApiKeyRequestFilter filter = new ApiKeyRequestFilter(apiKey);
        filter.doFilter(req, res, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
    }

    @Test
    void shouldNotPassTheFilterWhenFilterIsActiveAndApiKeyIsIncorrect() throws Exception {
        ApiKeyRequestFilter filter = new ApiKeyRequestFilter(apiKey);
        req.addHeader("X-API-KEY", "INCORRECT API KEY");
        filter.doFilter(req, res, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
    }
}
