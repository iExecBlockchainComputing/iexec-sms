package com.iexec.sms.blockchain;

import com.iexec.common.chain.Web3jAbstractService;
import org.springframework.stereotype.Service;

@Service
public class Web3jService extends Web3jAbstractService {

    public Web3jService(BlockchainConfig blockchainConfig) {
        super(
                blockchainConfig.getNodeAddress(),
                blockchainConfig.getGasPriceMultiplier(),
                blockchainConfig.getGasPriceCap(),
                blockchainConfig.isSidechain()
        );
    }

}