

package org.dubhe.data.client;

import org.dubhe.biz.base.constant.ApplicationNameConst;
import org.dubhe.biz.base.dto.TrainAlgorithmSelectAllBatchIdDTO;
import org.dubhe.biz.base.dto.TrainAlgorithmSelectByIdDTO;
import org.dubhe.biz.base.dto.TrainAlgorithmSelectByNameDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.base.vo.TrainAlgorithmQureyVO;
import org.dubhe.data.client.fallback.AlgorithmClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(value = ApplicationNameConst.SERVER_ALGORITHM,contextId = "algorithmClient",fallback = AlgorithmClientFallback.class)
public interface AlgorithmClient {

    /**
     * 根据算法id查询算法
     * @param trainAlgorithmSelectByIdDTO 算法查询参数
     * @return DataResponseBody
     */
    @GetMapping("/algorithms/selectById")
    DataResponseBody<TrainAlgorithmQureyVO> selectById(@SpringQueryMap TrainAlgorithmSelectByIdDTO trainAlgorithmSelectByIdDTO);

    /**
     * 根据算法名称模糊查询算法id
     * @param trainAlgorithmSelectByNameDTO 算法查询参数
     * @return DataResponseBody
     */
    @GetMapping("/algorithms/listIdByName")
    DataResponseBody<List<Long>> listIdByName(@SpringQueryMap TrainAlgorithmSelectByNameDTO trainAlgorithmSelectByNameDTO);



    /**
     * 根据算法id查询算法
     * @param trainAlgorithmSelectAllBatchIdDTO 算法查询参数
     * @return DataResponseBody
     */
    @GetMapping("/algorithms/selectAllBatchIds")
    DataResponseBody<List<TrainAlgorithmQureyVO>> selectAllBatchIds(@SpringQueryMap TrainAlgorithmSelectAllBatchIdDTO trainAlgorithmSelectAllBatchIdDTO);

}