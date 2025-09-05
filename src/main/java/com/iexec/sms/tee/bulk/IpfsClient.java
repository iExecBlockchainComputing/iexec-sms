package com.iexec.sms.tee.bulk;

import com.iexec.commons.poco.bulk.DatasetCid;
import com.iexec.commons.poco.order.DatasetOrder;
import feign.Param;
import feign.RequestLine;

import java.util.List;
import java.util.Map;

public interface IpfsClient {
    @RequestLine("GET /ipfs/{cid}")
    Map<Integer, DatasetCid> readBulkCid(@Param("cid") final String cid);

    @RequestLine("GET /ipfs/{cid}")
    List<DatasetOrder> readOrders(@Param("cid") final String cid);
}
