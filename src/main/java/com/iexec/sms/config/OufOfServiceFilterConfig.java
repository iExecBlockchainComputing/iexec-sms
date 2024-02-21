/*
 * Copyright 2024-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.sms.admin.AdminService;
import com.iexec.sms.admin.OutOfServiceRequestFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable OutOfService filter.
 * The filter will be activated only if the configuration is enabled.
 * In addition, the endpoints /admin, /actuator/*, /version, /metrics, /swagger-ui/index.html, /v3/api-docs
 * are out of the filter scope and will always be served.
 * The main purpose of activating this filter is to prevent database insertion during a restore.
 */
@Configuration
@ConditionalOnExpression("'${admin.out-of-service.enabled}'=='true'")
public class OufOfServiceFilterConfig {

    @Bean
    public FilterRegistrationBean<OutOfServiceRequestFilter> oufOfServiceFilterRegistrationBean(AdminService adminService) {
        FilterRegistrationBean<OutOfServiceRequestFilter> registrationBean = new FilterRegistrationBean<>();
        OutOfServiceRequestFilter outOfServiceRequestFilter = new OutOfServiceRequestFilter(adminService);

        registrationBean.setFilter(outOfServiceRequestFilter);
        registrationBean.addUrlPatterns("/tee/*", "/secrets/*", "/apps/*", "/requesters/*");
        return registrationBean;
    }
}
