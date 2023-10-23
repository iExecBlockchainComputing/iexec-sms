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

import lombok.Getter;
import org.springframework.web.filter.GenericFilterBean;

import lombok.extern.slf4j.Slf4j;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * ApiKeyRequestFilter is a security filter that can be activated to protect endpoints.
 * <p>
 * It is based on the use of an API Key that the caller must fill in via the X-API-KEY header.
 * If this filter is activated and the caller does not enter an API Key, then a 401 will be raised
 */
@Slf4j
public class ApiKeyRequestFilter  extends GenericFilterBean {


    private static final String API_KEY_HEADER_NAME = "X-API-KEY"; //Name of header in which api key is expected
    @Getter
    private String apiKey = ""; //The filter API Key
    @Getter
    private boolean isEnabled = false;

    public ApiKeyRequestFilter(String apiKey){
        if (null != apiKey && !apiKey.isBlank()){
            this.apiKey = apiKey;
            this.isEnabled = true;
        }else{
            log.warn("API Key filter is not enabled");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (this.isEnabled()) {
            HttpServletRequest req = (HttpServletRequest) request;
            String key = req.getHeader(API_KEY_HEADER_NAME) == null ? "" : req.getHeader(API_KEY_HEADER_NAME);
            if ( key.isBlank() || !key.equalsIgnoreCase(this.getApiKey()) ) {
                HttpServletResponse resp = (HttpServletResponse) response;
                String error = "You are not authorized to access this endpoint";

                resp.reset();
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentLength(error.length());
                response.getWriter().write(error);
            }else{
                chain.doFilter(request, response);
            }
        }else{
            chain.doFilter(request, response);
        }
    }
}
