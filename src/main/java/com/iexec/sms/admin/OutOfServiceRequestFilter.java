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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * This filter can be used to disable incoming requests.
 */
public class OutOfServiceRequestFilter extends GenericFilterBean {

    public static final String MAINTENANCE_ERROR = "The server is temporarily unable to serve your request due to maintenance downtime";
    private final AdminService adminService;

    public OutOfServiceRequestFilter(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //SMS is online, we let the requests through
        if (adminService.isSmsOnline()) {
            chain.doFilter(request, response);
        } else {
            //otherwise we systematically return a 503
            final HttpServletResponse resp = (HttpServletResponse) response;
            resp.reset();
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentLength(MAINTENANCE_ERROR.length());
            response.getWriter().write(MAINTENANCE_ERROR);
        }
    }
}
