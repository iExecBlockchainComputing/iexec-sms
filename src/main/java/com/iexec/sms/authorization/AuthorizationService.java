package com.iexec.sms.authorization;


import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.ChainTaskStatus;
import com.iexec.common.security.Signature;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.HashUtils;
import com.iexec.common.utils.SignatureUtils;
import com.iexec.sms.blockchain.IexecHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.iexec.common.utils.BytesUtils.stringToBytes;
import static com.iexec.sms.App.DOMAIN;

@Slf4j
@Service
public class AuthorizationService {


    private IexecHubService iexecHubService;

    public AuthorizationService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    public boolean isAuthorizedOnExecution(Authorization authorization, boolean isTeeEndpoint) {
        if (authorization == null || authorization.getChainTaskId().isEmpty()) {
            log.error("isAuthorizedOnExecution failed (empty params)");
            return false;
        }
        String chainTaskId = authorization.getChainTaskId();

        boolean isAllowedToAccessEndpoint = isTeeEndpoint == iexecHubService.isTeeTask(chainTaskId);
        if (!isAllowedToAccessEndpoint) {
            log.error("isAuthorizedOnExecution failed (unauthorized endpoint) [chainTaskId:{}]", chainTaskId);
            return false;
        }

        Optional<ChainTask> optionalChainTask = iexecHubService.getChainTask(chainTaskId);
        if (!optionalChainTask.isPresent()) {
            log.error("isAuthorizedOnExecution failed (getChainTask failed) [chainTaskId:{}]", chainTaskId);
            return false;
        }
        ChainTask chainTask = optionalChainTask.get();
        ChainTaskStatus taskStatus = chainTask.getStatus();
        String chainDealId = chainTask.getDealid();

        if (!taskStatus.equals(ChainTaskStatus.ACTIVE)) {
            log.error("isAuthorizedOnExecution failed (task not active) [chainTaskId:{}, status:{}]", chainTaskId, taskStatus);
            return false;
        }

        Optional<ChainDeal> optionalChainDeal = iexecHubService.getChainDeal(chainDealId);
        if (!optionalChainDeal.isPresent()) {
            log.error("isAuthorizedOnExecution failed (getChainDeal failed) [chainTaskId:{}]", chainTaskId);
            return false;
        }
        ChainDeal chainDeal = optionalChainDeal.get();
        String workerpoolAddress = chainDeal.getPoolOwner();

        boolean isWorkerSignatureValid = isWorkerSignatureOfAuthorizationValid(authorization);

        boolean isWorkerpoolSignatureValid = isWorkerpoolSignatureOfAuthorizationValid(authorization, workerpoolAddress);

        if (isWorkerSignatureValid && isWorkerpoolSignatureValid) {
            return true;
        }

        log.error("isAuthorizedOnExecution failed (invalid signature) [chainTaskId:{}, isWorkerSignatureValid:{}, " +
                "isWorkerpoolSignatureValid:{}]", chainTaskId, isWorkerSignatureValid, isWorkerpoolSignatureValid);
        return false;
    }

    private boolean isWorkerSignatureOfAuthorizationValid(Authorization authorization) {
        if (authorization.getAuthorizationHash().isEmpty() || authorization.getWorkerSignature() == null ||
                authorization.getWorkerAddress().isEmpty()) {
            log.error("isWorkerSignatureOfAuthorizationValid failed (empty params) [chainTaskId:{}", authorization.getChainTaskId());
            return false;
        }
        return SignatureUtils.isSignatureValid(
                stringToBytes(authorization.getAuthorizationHash()),
                authorization.getWorkerSignature(),
                authorization.getWorkerAddress());
    }

    private boolean isWorkerpoolSignatureOfAuthorizationValid(Authorization authorization, String workerpoolAddress) {
        if (authorization.getAuthorizationHash().isEmpty() || authorization.getWorkerpoolSignature() == null ||
                workerpoolAddress.isEmpty()) {
            log.error("isWorkerpoolSignatureOfAuthorizationValid failed (empty params) [chainTaskId:{}", authorization.getChainTaskId());
            return false;
        }
        return SignatureUtils.isSignatureValid(
                stringToBytes(authorization.getAuthorizationHash()),
                authorization.getWorkerpoolSignature(),
                workerpoolAddress);
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

    public String getChallengeForGetWeb2Secret(String ownerAddress,
                                               String secretAddress) {
        return HashUtils.concatenateAndHash(
                DOMAIN,
                ownerAddress,
                HashUtils.sha256(secretAddress));
    }

    public String getChallengeForSetWeb3Secret(String secretAddress,
                                               String secretValue) {
        return HashUtils.concatenateAndHash(
                DOMAIN,
                secretAddress,
                secretValue);
    }

    public String getChallengeForSetWeb2Secret(String ownerAddress,
                                               String secretKey,
                                               String secretValue) {
        return HashUtils.concatenateAndHash(
                DOMAIN,
                ownerAddress,
                secretKey,
                secretValue);
    }
}
