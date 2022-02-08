/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface SmsClient {

    @RequestLine("POST /apps/{appAddress}/secrets/{secretIndex}")
    @Headers("Authorization: {authorization}")
    String addAppDeveloperAppComputeSecret(
            @Param("authorization") String authorization,
            @Param("appAddress") String appAddress,
            @Param("secretIndex") String secretIndex,
            String secretValue
    );

    @RequestLine("HEAD /apps/{appAddress}/secrets/{secretIndex}")
    String isAppDeveloperAppComputeSecretPresent(
            @Param("appAddress") String appAddress,
            @Param("secretIndex") String secretIndex
    );

    @RequestLine("POST /apps/{appAddress}/requesters/secrets-count")
    @Headers("Authorization: {authorization}")
    String setMaxRequesterSecretCountForAppCompute(
            @Param("authorization") String authorization,
            @Param("appAddress") String appAddress,
            int secretCount);

    @RequestLine("POST /requesters/{requesterAddress}/secrets/{secretKey}")
    @Headers("Authorization: {authorization}")
    String addRequesterAppComputeSecret(
            @Param("authorization") String authorization,
            @Param("requesterAddress") String requesterAddress,
            @Param("secretKey") String secretKey,
            String secretValue
    );

    @RequestLine("HEAD /requesters/{requesterAddress}/secrets/{secretKey}")
    String isRequesterAppComputeSecretPresent(
            @Param("requesterAddress") String requesterAddress,
            @Param("secretKey") String secretKey
    );

    @RequestLine("POST /secrets/web2?ownerAddress={ownerAddress}&secretName={secretName}")
    @Headers("Authorization: {authorization}")
    String setWeb2Secret(
            @Param("authorization") String authorization,
            @Param("ownerAddress") String ownerAddress,
            @Param("secretName") String secretName,
            String secretValue
    );

    @RequestLine("POST /secrets/web3?secretAddress={secretAddress}")
    @Headers("Authorization: {authorization}")
    String setWeb3Secret(
            @Param("authorization") String authorization,
            @Param("secretAddress") String secretAddress,
            String secretValue
    );

}
