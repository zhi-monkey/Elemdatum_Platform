


package org.dubhe.data.client;

import org.dubhe.biz.base.constant.ApplicationNameConst;
import org.dubhe.biz.base.dto.PtTrainDataSourceStatusQueryDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.client.fallback.TrainServerFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * @description feign调用训练服务接口
 * @date 2020-11-04
 */
@FeignClient(value = ApplicationNameConst.SERVER_TRAIN, fallback = TrainServerFallback.class)
public interface TrainServerClient {

    /**
     * 数据集状态展示
     *
     * @param ptTrainDataSourceStatusQueryDTO 查询数据集对应训练状态查询条件
     * @return
     */
    @GetMapping("/trainJob/dataSourceStatus")
    public DataResponseBody<Map<String, Boolean>> getTrainDataSourceStatus(@Validated @SpringQueryMap PtTrainDataSourceStatusQueryDTO ptTrainDataSourceStatusQueryDTO);

}
