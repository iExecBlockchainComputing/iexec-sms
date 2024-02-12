/*
 * Copyright 2023-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.config;

import com.iexec.sms.admin.ApiKeyRequestFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression(
        "T(org.apache.commons.lang3.StringUtils).isNotEmpty('${admin.api-key:}')"
)
public class ApiKeyFilterConfig {

    @Bean
    public FilterRegistrationBean<ApiKeyRequestFilter> filterRegistrationBean(@Value("${admin.api-key}") String apiKey) {
        FilterRegistrationBean<ApiKeyRequestFilter> registrationBean = new FilterRegistrationBean<>();
        ApiKeyRequestFilter apiKeyRequestFilter = new ApiKeyRequestFilter(apiKey);

        registrationBean.setFilter(apiKeyRequestFilter);
        registrationBean.addUrlPatterns("/admin/*");
        return registrationBean;
    }
}
