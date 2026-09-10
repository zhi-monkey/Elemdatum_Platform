

package org.dubhe.data.client.fallback;

import org.dubhe.biz.base.dto.TrainAlgorithmSelectAllBatchIdDTO;
import org.dubhe.biz.base.dto.TrainAlgorithmSelectByIdDTO;
import org.dubhe.biz.base.dto.TrainAlgorithmSelectByNameDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.base.vo.TrainAlgorithmQureyVO;
import org.dubhe.biz.dataresponse.factory.DataResponseFactory;
import org.dubhe.data.client.AlgorithmClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlgorithmClientFallback implements AlgorithmClient {

    @Override
    public DataResponseBody<TrainAlgorithmQureyVO> selectById(TrainAlgorithmSelectByIdDTO trainAlgorithmSelectByIdDTO) {
       return DataResponseFactory.failed( "call dubhe-algorithm server selectById error");
    }

    @Override
    public DataResponseBody<List<Long>> listIdByName(TrainAlgorithmSelectByNameDTO trainAlgorithmSelectByNameDTO) {
        return DataResponseFactory.failed( "call dubhe-algorithm server selectById error");
    }

    @Override
    public DataResponseBody<List<TrainAlgorithmQureyVO>> selectAllBatchIds(TrainAlgorithmSelectAllBatchIdDTO trainAlgorithmSelectAllBatchIdDTO) {
        return DataResponseFactory.failed( "call dubhe-algorithm server selectAllBatchIds error");
    }
}
