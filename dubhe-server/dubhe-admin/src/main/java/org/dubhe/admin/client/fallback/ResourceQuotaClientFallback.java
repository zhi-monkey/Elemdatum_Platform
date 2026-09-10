
package org.dubhe.admin.client.fallback;

import org.dubhe.admin.client.ResourceQuotaClient;
import org.dubhe.admin.domain.dto.UserConfigDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.dataresponse.factory.DataResponseFactory;

/**
 * @description ResourceQuotaClient 熔断处理
 * @date 2021-7-21
 */
public class ResourceQuotaClientFallback implements ResourceQuotaClient {
    @Override
    public DataResponseBody updateResourceQuota(UserConfigDTO userConfigDTO) {
        return DataResponseFactory.failed("Call ResourceQuota server updateResourceQuota error");
    }
}
