/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.sms.authorization;

import com.iexec.commons.poco.chain.ChainDeal;
import com.iexec.commons.poco.chain.ChainTask;
import com.iexec.commons.poco.chain.ChainTaskStatus;
import com.iexec.commons.poco.chain.WorkerpoolAuthorization;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.tee.TeeUtils;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.HashUtils;
import com.iexec.commons.poco.utils.SignatureUtils;
import com.iexec.sms.blockchain.IexecHubService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.util.Optional;

import static com.iexec.sms.App.DOMAIN;
import static com.iexec.sms.authorization.AuthorizationError.*;

@Slf4j
@Service
public class AuthorizationService {

    private final IexecHubService iexecHubService;

    public AuthorizationService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    /**
     * Checks whether this execution is authorized.
     *
     * @param workerpoolAuthorization The workerpool authorization to check
     * @return {@code Optional.empty()} if all checks passed, the failure reason otherwise
     */
    public Optional<AuthorizationError> isAuthorizedOnExecutionWithDetailedIssue(WorkerpoolAuthorization workerpoolAuthorization) {
        if (workerpoolAuthorization == null || StringUtils.isEmpty(workerpoolAuthorization.getChainTaskId())) {
            log.error("Not authorized with empty params");
            return Optional.of(EMPTY_PARAMS_UNAUTHORIZED);
        }

        final String chainTaskId = workerpoolAuthorization.getChainTaskId();
        final ChainTask chainTask = iexecHubService.getChainTask(chainTaskId).orElse(null);
        if (chainTask == null) {
            log.error("Could not get chainTask [chainTaskId:{}]", chainTaskId);
            return Optional.of(GET_CHAIN_TASK_FAILED);
        }
        final String chainDealId = chainTask.getDealid();

        if (chainTask.getStatus() != ChainTaskStatus.ACTIVE) {
            log.error("Task not active on chain [chainTaskId:{}, status:{}]",
                    chainTaskId, chainTask.getStatus());
            return Optional.of(TASK_NOT_ACTIVE);
        }

        final ChainDeal chainDeal = iexecHubService.getChainDeal(chainDealId).orElse(null);
        if (chainDeal == null) {
            log.error("isAuthorizedOnExecution failed (getChainDeal failed) [chainTaskId:{}]", chainTaskId);
            return Optional.of(GET_CHAIN_DEAL_FAILED);
        }

        final boolean isTeeTaskOnchain = TeeUtils.isTeeTag(chainDeal.getTag());
        if (!isTeeTaskOnchain) {
            log.error("Could not match onchain task type [isTeeTaskOnchain:{}, chainTaskId:{}]",
                    isTeeTaskOnchain, chainTaskId);
            return Optional.of(NO_MATCH_ONCHAIN_TYPE);
        }

        final boolean isSignedByWorkerpool = isSignedByHimself(workerpoolAuthorization.getHash(),
                workerpoolAuthorization.getSignature().getValue(), chainDeal.getPoolOwner());

        if (!isSignedByWorkerpool) {
            log.error("isAuthorizedOnExecution failed (invalid signature) [chainTaskId:{}, isSignedByWorkerpool:{}]",
                    chainTaskId, isSignedByWorkerpool);
            return Optional.of(INVALID_SIGNATURE);
        }

        return Optional.empty();
    }

    // region isSignedBy
    public boolean isSignedByHimself(String message, String signature, String address) {
        return SignatureUtils.isSignatureValid(BytesUtils.stringToBytes(message), new Signature(signature), address);
    }

    public boolean isSignedByOwner(String message, String signature, String address) {
        String owner = iexecHubService.getOwner(address);
        return !owner.isEmpty() && isSignedByHimself(message, signature, owner);
    }
    // endregion

    // region challenges
    public String getChallengeForSetAppDeveloperAppComputeSecret(String appAddress,
                                                                 String secretIndex,
                                                                 String secretValue) {
        return HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                appAddress,
                Hash.sha3String(secretIndex),
                Hash.sha3String(secretValue));
    }

    public String getChallengeForSetRequesterAppComputeSecret(
            String requesterAddress,
            String secretKey,
            String secretValue) {
        return HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                requesterAddress,
                Hash.sha3String(secretKey),
                Hash.sha3String(secretValue));
    }

    public String getChallengeForSetWeb2Secret(String ownerAddress,
                                               String secretKey,
                                               String secretValue) {
        return HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                ownerAddress,
                Hash.sha3String(secretKey),
                Hash.sha3String(secretValue));
    }

    public String getChallengeForSetWeb3Secret(String secretAddress,
                                               String secretValue) {
        return HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                secretAddress,
                Hash.sha3String(secretValue));
    }

    public String getChallengeForWorker(WorkerpoolAuthorization workerpoolAuthorization) {
        return HashUtils.concatenateAndHash(
                workerpoolAuthorization.getWorkerWallet(),
                workerpoolAuthorization.getChainTaskId(),
                workerpoolAuthorization.getEnclaveChallenge());
    }
    // endregion
}
