package com.iexec.sms.iexecsms.authorization;


import com.iexec.common.chain.ChainDeal;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.ChainTaskStatus;
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

    public boolean isAuthorizedToGetKeys(Authorization authorization) {
        if (authorization == null || authorization.getChainTaskId().isEmpty()) {
            log.error("isAuthorizedToGetKeys failed (empty params)");
            return false;
        }
        String chainTaskId = authorization.getChainTaskId();
        Optional<ChainTask> optionalChainTask = iexecHubService.getChainTask(chainTaskId);
        if (optionalChainTask.isEmpty()) {
            log.error("isAuthorizedToGetKeys failed (getChainTask failed) [chainTaskId:{}]", chainTaskId);
            return false;
        }
        ChainTask chainTask = optionalChainTask.get();
        ChainTaskStatus taskStatus = chainTask.getStatus();
        String chainDealId = chainTask.getDealid();

        if (!taskStatus.equals(ChainTaskStatus.ACTIVE)) {
            log.error("isAuthorizedToGetKeys failed (task not active) [chainTaskId:{}, status:{}]", chainTaskId, taskStatus);
            return false;
        }

        Optional<ChainDeal> optionalChainDeal = iexecHubService.getChainDeal(chainDealId);
        if (optionalChainDeal.isEmpty()) {
            log.error("isAuthorizedToGetKeys failed (getChainDeal failed) [chainTaskId:{}]", chainTaskId);
            return false;
        }
        ChainDeal chainDeal = optionalChainDeal.get();
        String workerpoolAddress = chainDeal.getPoolOwner();

        boolean isWorkerSignatureValid = isWorkerSignatureOfAuthorizationValid(authorization);

        boolean isWorkerpoolSignatureValid = isWorkerpoolSignatureOfAuthorizationValid(authorization, workerpoolAddress);

        if (isWorkerSignatureValid && isWorkerpoolSignatureValid) {
            return true;
        }

        log.error("isAuthorizedToGetKeys failed (invalid signature) [chainTaskId:{}, isWorkerSignatureValid:{}, " +
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


}
