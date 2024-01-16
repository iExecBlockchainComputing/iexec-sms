/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

    public boolean isAuthorizedOnExecution(WorkerpoolAuthorization workerpoolAuthorization, boolean isTeeTask) {
        return isAuthorizedOnExecutionWithDetailedIssue(workerpoolAuthorization, isTeeTask).isEmpty();
    }

    /**
     * Checks whether this execution is authorized.
     * If not authorized, return the reason.
     * Otherwise, returns an empty {@link Optional}.
     */
    public Optional<AuthorizationError> isAuthorizedOnExecutionWithDetailedIssue(WorkerpoolAuthorization workerpoolAuthorization, boolean isTeeTask) {
        if (workerpoolAuthorization == null || workerpoolAuthorization.getChainTaskId().isEmpty()) {
            log.error("Not authorized with empty params");
            return Optional.of(EMPTY_PARAMS_UNAUTHORIZED);
        }

        String chainTaskId = workerpoolAuthorization.getChainTaskId();
        Optional<ChainTask> optionalChainTask = iexecHubService.getChainTask(chainTaskId);
        if (optionalChainTask.isEmpty()) {
            log.error("Could not get chainTask [chainTaskId:{}]", chainTaskId);
            return Optional.of(GET_CHAIN_TASK_FAILED);
        }
        ChainTask chainTask = optionalChainTask.get();
        ChainTaskStatus taskStatus = chainTask.getStatus();
        String chainDealId = chainTask.getDealid();

        if (!taskStatus.equals(ChainTaskStatus.ACTIVE)) {
            log.error("Task not active onchain [chainTaskId:{}, status:{}]",
                    chainTaskId, taskStatus);
            return Optional.of(TASK_NOT_ACTIVE);
        }

        Optional<ChainDeal> optionalChainDeal = iexecHubService.getChainDeal(chainDealId);
        if (optionalChainDeal.isEmpty()) {
            log.error("isAuthorizedOnExecution failed (getChainDeal failed) [chainTaskId:{}]", chainTaskId);
            return Optional.of(GET_CHAIN_DEAL_FAILED);
        }
        ChainDeal chainDeal = optionalChainDeal.get();

        boolean isTeeTaskOnchain = TeeUtils.isTeeTag(chainDeal.getTag());
        if (isTeeTask != isTeeTaskOnchain) {
            log.error("Could not match onchain task type [isTeeTask:{}, isTeeTaskOnchain:{},"
                            + "chainTaskId:{}, walletAddress:{}]", isTeeTask, isTeeTaskOnchain,
                    chainTaskId, workerpoolAuthorization.getWorkerWallet());
            return Optional.of(NO_MATCH_ONCHAIN_TYPE);
        }

        String workerpoolAddress = chainDeal.getPoolOwner();
        boolean isSignerByWorkerpool = isSignedByHimself(workerpoolAuthorization.getHash(),
                workerpoolAuthorization.getSignature().getValue(), workerpoolAddress);

        if (!isSignerByWorkerpool) {
            log.error("isAuthorizedOnExecution failed (invalid signature) [chainTaskId:{}, isWorkerpoolSignatureValid:{}]",
                    chainTaskId, isSignerByWorkerpool);
            return Optional.of(INVALID_SIGNATURE);
        }

        return Optional.empty();
    }

    public boolean isSignedByHimself(String message, String signature, String address) {
        return SignatureUtils.isSignatureValid(BytesUtils.stringToBytes(message), new Signature(signature), address);
    }

    public boolean isSignedByOwner(String message, String signature, String address) {
        String owner = iexecHubService.getOwner(address);
        return !owner.isEmpty() && isSignedByHimself(message, signature, owner);
    }

    public String getChallengeForSetWeb3Secret(String secretAddress,
                                               String secretValue) {
        return HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                secretAddress,
                Hash.sha3String(secretValue));
    }

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

    public String getChallengeForWorker(WorkerpoolAuthorization workerpoolAuthorization) {
        return HashUtils.concatenateAndHash(
                workerpoolAuthorization.getWorkerWallet(),
                workerpoolAuthorization.getChainTaskId(),
                workerpoolAuthorization.getEnclaveChallenge());
    }
}
