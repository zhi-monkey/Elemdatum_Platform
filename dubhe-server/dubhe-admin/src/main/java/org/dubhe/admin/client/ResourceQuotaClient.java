
package org.dubhe.admin.client;

import org.dubhe.admin.client.fallback.ResourceQuotaClientFallback;
import org.dubhe.admin.domain.dto.UserConfigDTO;
import org.dubhe.biz.base.constant.ApplicationNameConst;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @description 远程调用资源配额 Client
 * @date 2021-7-21
 */
@FeignClient(value = ApplicationNameConst.SERVER_K8S,fallback = ResourceQuotaClientFallback.class)
public interface ResourceQuotaClient {
    /**
     * 更新 ResourceQuota
     *
     * @param userConfigDTO 用户配置信息
     * @return
     */
    @PostMapping(value = "/resourceQuota/update")
    DataResponseBody updateResourceQuota(@RequestBody UserConfigDTO userConfigDTO);
}
