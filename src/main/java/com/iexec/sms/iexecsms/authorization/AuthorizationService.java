package com.iexec.sms.iexecsms.authorization;


import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.ChainTaskStatus;
import com.iexec.common.security.Signature;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.SignatureUtils;
import com.iexec.sms.iexecsms.blockchain.IexecHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.iexec.common.utils.BytesUtils.stringToBytes;

@Slf4j
@Service
public class AuthorizationService {


    private IexecHubService iexecHubService;

    public AuthorizationService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    public boolean isAuthorizedOnExecution(Authorization authorization) {
        if (authorization == null || authorization.getChainTaskId().isEmpty()) {
            log.error("isAuthorizedOnExecution failed (empty params)");
            return false;
        }
        String chainTaskId = authorization.getChainTaskId();
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

    public boolean isAuthorized(String message, String signature, String address) {
        if (isSignedByHimself(message, signature, address)) {
            log.info("Signature is authorized for ownerAddress (self) [ownerAddress:{}", address);
            return true;
        } else if (isSignedByOwner(message, signature, address)) {
            log.info("Signature is authorized for ownerAddress (owner) [ownerAddress:{}", address);
            return true;
        }
        log.error("Address cant be authorized to push [ownerAddress:{}", address);
        return false;
    }

    public boolean isSignedByHimself(String message, String signature, String address) {
        return SignatureUtils.isSignatureValid(BytesUtils.stringToBytes(message), new Signature(signature), address);
    }

    public boolean isSignedByOwner(String message, String signature, String address) {
        String owner = iexecHubService.getOwner(address);
        if (!owner.isEmpty() && isSignedByHimself(message, signature, owner)) {
            return true;
        }
        return false;
    }


}
