package org.dubhe.admin.client.fallback;

import org.dubhe.admin.client.DubheDataClient;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.vo.DataResponseBody;

public class DubheDataClientFallBack implements DubheDataClient {

    @Override
    public DataResponseBody handleUserRemoval(Long userId){
        return new DataResponseBody(ResponseCode.ERROR,"Dubhe-data feign remove error");
    }

    @Override
    public DataResponseBody createNotification(Long toUserId, Integer notificationType, String operationType, String payload) {
        return new DataResponseBody(ResponseCode.ERROR, "Dubhe-data feign notification error");
    }
}
