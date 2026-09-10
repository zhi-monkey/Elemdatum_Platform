package org.dubhe.admin.client;

import org.dubhe.admin.client.fallback.DubheDataClientFallBack;
import org.dubhe.admin.domain.dto.UserConfigDTO;
import org.dubhe.biz.base.constant.ApplicationNameConst;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author 10230
 */
@FeignClient(value = ApplicationNameConst.SERVER_DATA,contextId = "DubheDataClient",fallback = DubheDataClientFallBack.class)
public interface DubheDataClient {

    @PostMapping(value =  "/datasets/team/handleUserRemoval")
    DataResponseBody handleUserRemoval(@RequestParam(value = "userId") Long userId);

    /**
     * 创建通知
     */
    @PostMapping(value = "/notifications/createAsync")
    DataResponseBody createNotification(@RequestParam(value = "toUserId") Long toUserId,
                                        @RequestParam(value = "notificationType") Integer notificationType,
                                        @RequestParam(value = "operationType") String operationType,
                                        @RequestBody String payload);
}
