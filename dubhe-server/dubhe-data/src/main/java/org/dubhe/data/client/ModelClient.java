

package org.dubhe.data.client;

import org.dubhe.biz.base.constant.ApplicationNameConst;
import org.dubhe.biz.base.dto.PtModelBranchQueryByIdDTO;
import org.dubhe.biz.base.dto.PtModelBranchQueryByIdsDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.base.vo.PtModelBranchQueryVO;
import org.dubhe.data.client.fallback.ModelClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(value = ApplicationNameConst.SERVER_MODEL, contextId = "modelClient", fallback = ModelClientFallback.class)
public interface ModelClient {
    /**
     * 根据模型版本id查询模型版本详情
     *
     * @param ptModelBranchQueryByIdDTO 模型版本详情查询条件
     * @return PtModelBranchQueryByIdVO 模型版本详情
     */
    @GetMapping("/ptModelBranch/byBranchId")
    DataResponseBody<PtModelBranchQueryVO> getByBranchId(@SpringQueryMap PtModelBranchQueryByIdDTO ptModelBranchQueryByIdDTO);


    /**
     * 根据模型版本id查询模型版本详情
     *
     * @param ptModelBranchQueryByIdsDTO 模型版本详情查询条件
     * @return PtModelBranchQueryByIdVOs 模型版本详情
     */
    @GetMapping("/ptModelBranch/listByBranchIds")
    DataResponseBody<List<PtModelBranchQueryVO>> listByBranchIds(@SpringQueryMap PtModelBranchQueryByIdsDTO ptModelBranchQueryByIdsDTO);


}
