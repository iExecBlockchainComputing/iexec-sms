/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.api;

import com.iexec.common.utils.FeignBuilder;
import feign.Logger;
import feign.Request;
import feign.Retryer;

import java.util.concurrent.TimeUnit;

/**
 * Creates Feign client instances to query REST endpoints described in {@link SmsClient}.
 * @see FeignBuilder
 */
public class SmsClientBuilder {

    private SmsClientBuilder() {}

    /**
     * Create an unauthenticated feign client to query apis described in {@link SmsClient}.
     * @param logLevel Feign logging level to configure.
     * @param url Url targeted by the client.
     * @return Feign client for {@link SmsClient} apis.
     */
    public static SmsClient getInstance(Logger.Level logLevel, String url) {
        return FeignBuilder.createBuilder(logLevel)
                .retryer(Retryer.NEVER_RETRY)
                .options(new Request.Options(10, TimeUnit.MINUTES, 30, TimeUnit.MINUTES, true))
                .target(SmsClient.class, url);
    }

}
