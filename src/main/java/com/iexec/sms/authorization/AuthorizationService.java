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

package com.iexec.sms.authorization;


import static com.iexec.sms.App.DOMAIN;

import java.util.Optional;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.ChainTaskStatus;
import com.iexec.common.chain.WorkerpoolAuthorization;
import com.iexec.common.security.Signature;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.HashUtils;
import com.iexec.common.utils.SignatureUtils;
import com.iexec.sms.blockchain.IexecHubService;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Hash;

@Slf4j
@Service
public class AuthorizationService {


    private IexecHubService iexecHubService;

    public AuthorizationService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    public boolean isAuthorizedOnExecution(WorkerpoolAuthorization workerpoolAuthorization, boolean isTeeTask) {
        if (workerpoolAuthorization == null || workerpoolAuthorization.getChainTaskId().isEmpty()) {
            log.error("Not authorized with empty params");
            return false;
        }

        String chainTaskId = workerpoolAuthorization.getChainTaskId();
        boolean isTeeTaskOnchain = iexecHubService.isTeeTask(chainTaskId);
        if (isTeeTask != isTeeTaskOnchain) {
            log.error("Could not match onchain task type [isTeeTask:{}, isTeeTaskOnchain:{},"
                    + "chainTaskId:{}, walletAddress:{}]",isTeeTask, isTeeTaskOnchain,
                    chainTaskId, workerpoolAuthorization.getWorkerWallet());
            return false;
        }

        Optional<ChainTask> optionalChainTask = iexecHubService.getChainTask(chainTaskId);
        if (!optionalChainTask.isPresent()) {
            log.error("Could not get chainTask [chainTaskId:{}]", chainTaskId);
            return false;
        }
        ChainTask chainTask = optionalChainTask.get();
        ChainTaskStatus taskStatus = chainTask.getStatus();
        String chainDealId = chainTask.getDealid();

        if (!taskStatus.equals(ChainTaskStatus.ACTIVE)) {
            log.error("Task not active onchain [chainTaskId:{}, status:{}]",
                    chainTaskId, taskStatus);
            return false;
        }

        Optional<ChainDeal> optionalChainDeal = iexecHubService.getChainDeal(chainDealId);
        if (!optionalChainDeal.isPresent()) {
            log.error("isAuthorizedOnExecution failed (getChainDeal failed) [chainTaskId:{}]", chainTaskId);
            return false;
        }
        ChainDeal chainDeal = optionalChainDeal.get();
        String workerpoolAddress = chainDeal.getPoolOwner();
        boolean isSignerByWorkerpool = isSignedByHimself(workerpoolAuthorization.getHash(),
                workerpoolAuthorization.getSignature().getValue(), workerpoolAddress);

        if (!isSignerByWorkerpool) {
            log.error("isAuthorizedOnExecution failed (invalid signature) [chainTaskId:{}, isWorkerpoolSignatureValid:{}]",
                    chainTaskId, isSignerByWorkerpool);
            return false;
        }

        return true;
    }

    public boolean isSignedByHimself(String message, String signature, String address) {
        return SignatureUtils.isSignatureValid(BytesUtils.stringToBytes(message), new Signature(signature), address);
    }

    public boolean isSignedByOwner(String message, String signature, String address) {
        String owner = iexecHubService.getOwner(address);
        return !owner.isEmpty() && isSignedByHimself(message, signature, owner);
    }

    public String getChallengeForGetWeb3Secret(String secretAddress) {
        return HashUtils.concatenateAndHash(
                DOMAIN,
                secretAddress);
    }

    /*
    *  Note - These are equals:
    *  BytesUtils.bytesToString(Hash.sha3(DOMAIN.getBytes())
    *  Hash.sha3String(DOMAIN)
    * */
    public String getChallengeForGetWeb2Secret(String ownerAddress,
                                               String secretKey) {
        return HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                ownerAddress,
                Hash.sha3String(secretKey));
    }

    public String getChallengeForSetWeb3Secret(String secretAddress,
                                               String secretValue) {
        return HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                secretAddress,
                Hash.sha3String(secretValue));
    }

    public String getChallengeForSetRuntimeSecret(String appAddress,
                                                  long secretIndex,
                                                  String secretValue) {
        return HashUtils.concatenateAndHash(
                Hash.sha3String(DOMAIN),
                appAddress,
                Long.toString(secretIndex),
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
