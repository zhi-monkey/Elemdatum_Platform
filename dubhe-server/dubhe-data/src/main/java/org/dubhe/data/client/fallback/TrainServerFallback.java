


package org.dubhe.data.client.fallback;

import org.dubhe.biz.base.dto.PtTrainDataSourceStatusQueryDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.dataresponse.factory.DataResponseFactory;
import org.dubhe.data.client.TrainServerClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description Feign 熔断处理类
 * @date 2020-11-04
 */
@Component
public class TrainServerFallback implements TrainServerClient {


    /**
     * 数据集状态展示
     *
     * @param ptTrainDataSourceStatusQueryDTO 查询数据集对应训练状态查询条件
     * @return
     */
    @Override
    public DataResponseBody<Map<String, Boolean>> getTrainDataSourceStatus(PtTrainDataSourceStatusQueryDTO ptTrainDataSourceStatusQueryDTO) {
        return DataResponseFactory.failed("call train server getTrainDataSourceStatus error ");
    }
}
