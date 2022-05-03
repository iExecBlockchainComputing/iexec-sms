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

import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.common.tee.TeeWorkflowSharedConfiguration;
import com.iexec.common.utils.BytesUtils;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SmsService {

    private final SmsClient smsClient;

    private TeeWorkflowSharedConfiguration teeWorkflowConfiguration;

    public SmsService(SmsClient smsClient) {
        this.smsClient = smsClient;
    }

    public boolean addAppDeveloperAppComputeSecret(String authorization,
                                                   String appAddress,
                                                   //String secretIndex,
                                                   String secretValue) {
        try {
            smsClient.addAppDeveloperAppComputeSecret(authorization, appAddress, secretValue);
            return true;
        } catch (FeignException e) {
            log.error("addAppDeveloperAppComputeSecret failed", e);
        }
        return false;
    }

    public boolean isAppDeveloperAppComputeSecretPresent(String appAddress, String secretIndex) {
        try {
            smsClient.isAppDeveloperAppComputeSecretPresent(appAddress, secretIndex);
            return true;
        } catch(FeignException e) {
            log.error("isAppDeveloperAppComputeSecretPresent failed", e);
        }
        return false;
    }

    public Integer getMaxRequesterSecretCountForAppCompute(String appAddress) {
        try  {
            return smsClient.getMaxRequesterSecretCountForAppCompute(appAddress).getData();
        } catch (FeignException e) {
            log.error("getMaxRequesterSecretCountForAppCompute failed", e);
        }
        return null;
    }

    public boolean setMaxRequesterSecretCountForAppCompute(String authorization, String appAddress, int secretCount) {
        try  {
            smsClient.setMaxRequesterSecretCountForAppCompute(authorization, appAddress, secretCount);
            return true;
        } catch (FeignException e) {
            log.error("setMaxRequesterSecretCount failed", e);
        }
        return false;
    }

    public boolean addRequesterAppComputeSecret(String authorization,
                                                String requesterAddress,
                                                String secretKey,
                                                String secretValue) {
        try {
            smsClient.addRequesterAppComputeSecret(
                    authorization, requesterAddress, secretKey, secretValue);
            return true;
        } catch (FeignException e) {
            log.error("addRequesterAppComputeSecret failed", e);
        }
        return false;
    }

    public boolean isRequesterAppComputeSecretPresent(String requesterAddress, String requesterSecretKey) {
        try {
            smsClient.isRequesterAppComputeSecretPresent(requesterAddress, requesterSecretKey);
            return true;
        } catch(FeignException e) {
            log.error("isRequesterAppComputeSecretPresent failed", e);
            return false;
        }
    }

    public boolean setWeb2Secret(String authorization, String ownerAddress, String secretName, String secretValue) {
        try {
            smsClient.setWeb2Secret(authorization, ownerAddress, secretName, secretValue);
            return true;
        } catch (FeignException e) {
            log.error("setWeb2Secret failed", e);
        }
        return false;
    }

    public boolean setWeb3Secret(String authorization, String secretAddress, String secretValue) {
        try {
            smsClient.setWeb3Secret(authorization, secretAddress, secretValue);
            return true;
        } catch (FeignException e) {
            log.error("setWeb3Secret failed", e);
        }
        return false;
    }

    // region for core

    public Optional<String> getEnclaveChallenge(String chainTaskId, boolean isTeeEnabled) {
        return isTeeEnabled
                ? generateEnclaveChallenge(chainTaskId)
                : Optional.of(BytesUtils.EMPTY_ADDRESS);
    }

    public Optional<String> generateEnclaveChallenge(String chainTaskId) {
        try {
            String teeChallengePublicKey = smsClient.generateTeeChallenge(chainTaskId);
            if (teeChallengePublicKey == null || teeChallengePublicKey.isEmpty()) {
                log.error("An error occurred while getting teeChallengePublicKey [chainTaskId:{}]", chainTaskId);
                return Optional.empty();
            }
            return Optional.of(teeChallengePublicKey);
        } catch (FeignException e) {
            log.error("Tee challenge generation failed", e);
        }
        return Optional.empty();
    }

    // region for worker

    /**
     * Get the configuration needed for TEE workflow from the SMS. This
     * configuration contains: las image, pre-compute image uri, pre-compute heap
     * size, post-compute image uri, post-compute heap size.
     * Note: Caching response to avoid calling the SMS
     * @return configuration if success, null otherwise
     */
    public TeeWorkflowSharedConfiguration getTeeWorkflowConfiguration() {
        if (teeWorkflowConfiguration == null) {
            try {
                teeWorkflowConfiguration = smsClient.getTeeWorkflowConfiguration();
            } catch (FeignException e) {
                log.error("Failed to get tee workflow configuration from sms", e);
                teeWorkflowConfiguration = null;
            }
        }
        return teeWorkflowConfiguration;
    }

    public String getSconeCasUrl() {
        try {
            return smsClient.getSconeCasUrl();
        } catch(FeignException e) {
            log.error("Failed to get scone cas configuration from sms", e);
        }
        return "";
    }

    public String createTeeSession(String authorization, WorkerpoolAuthorization workerpoolAuthorization) {
        String chainTaskId = workerpoolAuthorization.getChainTaskId();
        log.info("Creating TEE session [chainTaskId:{}]", chainTaskId);
        try {
            String sessionId = smsClient.generateTeeSession(authorization, workerpoolAuthorization);
            log.info("Created TEE session [chainTaskId:{}, sessionId:{}]",
                    chainTaskId, sessionId);
            return sessionId;
        } catch(FeignException e) {
            log.error("SMS failed to create TEE session [chainTaskId:{}]",
                    chainTaskId, e);
        }
        return "";
    }

}
