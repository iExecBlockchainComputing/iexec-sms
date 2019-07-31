package com.iexec.sms.iexecsms.blockchain;

import com.iexec.common.chain.Web3jAbstractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Web3jService extends Web3jAbstractService {

    public Web3jService(BlockchainConfig blockchainConfig) {
        super(blockchainConfig.getNodeAddress(), blockchainConfig.getGasPriceMultiplier(), blockchainConfig.getGasPriceCap());
    }

}