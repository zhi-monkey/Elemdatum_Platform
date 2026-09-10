

package org.dubhe.data.client.fallback;

import org.dubhe.biz.base.dto.PtModelBranchQueryByIdDTO;
import org.dubhe.biz.base.dto.PtModelBranchQueryByIdsDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.base.vo.PtModelBranchQueryVO;
import org.dubhe.biz.dataresponse.factory.DataResponseFactory;
import org.dubhe.data.client.ModelClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ModelClientFallback implements ModelClient {


    @Override
    public DataResponseBody<PtModelBranchQueryVO> getByBranchId(PtModelBranchQueryByIdDTO ptModelBranchQueryByIdDTO) {
        return DataResponseFactory.failed("call dubhe-model server getByBranchId error ");
    }

    @Override
    public DataResponseBody<List<PtModelBranchQueryVO>> listByBranchIds(PtModelBranchQueryByIdsDTO ptModelBranchQueryByIdsDTO) {
        return DataResponseFactory.failed("call dubhe-model server listByBranchIds error ");
    }
}
