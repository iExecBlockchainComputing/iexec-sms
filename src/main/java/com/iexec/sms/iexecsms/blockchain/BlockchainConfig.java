package com.iexec.sms.iexecsms.blockchain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BlockchainConfig {

    @Value("${blockchain.id}")
    private Integer chainId;

    @Value("${blockchain.nodeAddress}")
    private String nodeAddress;

    @Value("${blockchain.hubAddress}")
    private String hubAddress;

    @Value("${blockchain.gasPriceMultiplier}")
    private float gasPriceMultiplier;

    @Value("${blockchain.gasPriceCap}")
    private long gasPriceCap;

}
