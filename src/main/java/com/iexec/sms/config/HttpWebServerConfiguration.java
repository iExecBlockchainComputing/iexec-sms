/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpWebServerConfiguration {

    @Value("${server.http.enabled}")
    private boolean isHttpEnabled;

    @Value("${server.http.port}")
    private int httpPort;

    /*
    * This method will allow http connections (without SSL) if set in configuration
    * */
    @Bean
    public ServletWebServerFactory servletContainer() {
        if (isHttpEnabled) {
            return getHttpWebServerFactory();
        }
        return getStandardWebServerFactory();
    }

    private TomcatServletWebServerFactory getHttpWebServerFactory() {
        TomcatServletWebServerFactory factory = getStandardWebServerFactory();
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(httpPort);
        factory.addAdditionalTomcatConnectors(connector);
        return factory;
    }

    private TomcatServletWebServerFactory getStandardWebServerFactory() {
        return new TomcatServletWebServerFactory();
    }

}