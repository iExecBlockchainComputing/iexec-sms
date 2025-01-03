/*
 * Copyright 2023-2025 IEXEC BLOCKCHAIN TECH
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * ApiKeyRequestFilter is a security filter that can be activated to protect endpoints.
 * <p>
 * It is based on the use of an API Key that the caller must fill in via the X-API-KEY header.
 * <p>
 * If an API Key is configured, the filter is activated and requests have to present a valid API Key,
 * if this is not the case, a 401 message is sent.
 * If no API Key is configured, then the filter forbids any request and returns a 403 status code.
 */
@Slf4j
public class ApiKeyRequestFilter extends GenericFilterBean {


    private static final String API_KEY_HEADER_NAME = "X-API-KEY"; //Name of header in which api key is expected
    private final String apiKey; //The filter API Key

    private final boolean isSensitiveApiDisabled;

    public ApiKeyRequestFilter(String apiKey) {
        this.isSensitiveApiDisabled = StringUtils.isBlank(apiKey);

        if (isSensitiveApiDisabled) {
            this.apiKey = null;
            log.warn("No API key has been configured. Sensitive API is therefore disabled.");
            return;
        }

        this.apiKey = apiKey;
        log.info("API key has been configured. Sensitive API is accessible using the API key.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (this.isSensitiveApiDisabled) {
            sendResponseWithStatusAndMessage(response, "This endpoint is disabled.", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final HttpServletRequest req = (HttpServletRequest) request;

        final String key = req.getHeader(API_KEY_HEADER_NAME);
        if (!this.apiKey.equals(key)) {
            sendResponseWithStatusAndMessage(response, "You are not authorized to access this endpoint.", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }

    private static void sendResponseWithStatusAndMessage(ServletResponse response, String error, int statusCode) throws IOException {
        final HttpServletResponse resp = (HttpServletResponse) response;

        resp.reset();
        resp.setStatus(statusCode);
        response.setContentLength(error.length());
        response.getWriter().write(error);
    }
}
