/*
 * Copyright 2024-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.admin;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
