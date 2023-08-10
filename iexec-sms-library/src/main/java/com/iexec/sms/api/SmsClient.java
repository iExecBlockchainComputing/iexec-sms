/*
 * Copyright 2022-2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.web.ApiResponseBody;
import com.iexec.commons.poco.chain.WorkerpoolAuthorization;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.sms.api.config.TeeServicesProperties;
import com.iexec.sms.metric.SmsMetrics;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

/**
 * Interface allowing to instantiate a Feign client targeting SMS REST endpoints.
 * <p>
 * To create the client, see the related builder.
 * @see SmsClientBuilder
 */
public interface SmsClient {

    @RequestLine("GET /up")
    String isUp();

    // region Secrets
    @RequestLine("POST /apps/{appAddress}/secrets/1")
    @Headers("Authorization: {authorization}")
    ApiResponseBody<String, List<String>> addAppDeveloperAppComputeSecret(
            @Param("authorization") String authorization,
            @Param("appAddress") String appAddress,
            //@Param("secretIndex") String secretIndex,
            String secretValue
    );

    @RequestLine("HEAD /apps/{appAddress}/secrets/{secretIndex}")
    ApiResponseBody<String, List<String>> isAppDeveloperAppComputeSecretPresent(
            @Param("appAddress") String appAddress,
            @Param("secretIndex") String secretIndex
    );

    @RequestLine("POST /requesters/{requesterAddress}/secrets/{secretKey}")
    @Headers("Authorization: {authorization}")
    ApiResponseBody<String, List<String>> addRequesterAppComputeSecret(
            @Param("authorization") String authorization,
            @Param("requesterAddress") String requesterAddress,
            @Param("secretKey") String secretKey,
            String secretValue
    );

    @RequestLine("HEAD /requesters/{requesterAddress}/secrets/{secretKey}")
    ApiResponseBody<String, List<String>> isRequesterAppComputeSecretPresent(
            @Param("requesterAddress") String requesterAddress,
            @Param("secretKey") String secretKey
    );

    @RequestLine("HEAD /secrets/web2?ownerAddress={ownerAddress}&secretName={secretName}")
    void isWeb2SecretSet(
            @Param("ownerAddress") String ownerAddress,
            @Param("secretName") String secretName
    );

    @RequestLine("POST /secrets/web2?ownerAddress={ownerAddress}&secretName={secretName}")
    @Headers("Authorization: {authorization}")
    String setWeb2Secret(
            @Param("authorization") String authorization,
            @Param("ownerAddress") String ownerAddress,
            @Param("secretName") String secretName,
            String secretValue
    );

    @RequestLine("PUT /secrets/web2?ownerAddress={ownerAddress}&secretName={secretName}")
    @Headers("Authorization: {authorization}")
    String updateWeb2Secret(
            @Param("authorization") String authorization,
            @Param("ownerAddress") String ownerAddress,
            @Param("secretName") String secretName,
            String secretValue
    );

    @RequestLine("HEAD /secrets/web3?secretAddress={secretAddress}")
    void isWeb3SecretSet(
            @Param("secretAddress") String secretAddress
    );

    @RequestLine("POST /secrets/web3?secretAddress={secretAddress}")
    @Headers("Authorization: {authorization}")
    String setWeb3Secret(
            @Param("authorization") String authorization,
            @Param("secretAddress") String secretAddress,
            String secretValue
    );
    // endregion

    // region TEE
    @RequestLine("POST /tee/challenges/{chainTaskId}")
    String generateTeeChallenge(@Param("chainTaskId") String chainTaskId);

    @RequestLine("POST /tee/sessions")
    @Headers("Authorization: {authorization}")
    ApiResponseBody<TeeSessionGenerationResponse, TeeSessionGenerationError> generateTeeSession(
            @Param("authorization") String authorization,
            WorkerpoolAuthorization workerpoolAuthorization
    );

    @RequestLine("GET /tee/framework")
    TeeFramework getTeeFramework();

    @RequestLine("GET /tee/properties/{teeFramework}")
    <T extends TeeServicesProperties> T getTeeServicesProperties(@Param("teeFramework") TeeFramework teeFramework);
    // endregion

    // region Metrics
    @RequestLine("GET /metrics")
    SmsMetrics getMetrics();
    // endregion
}
