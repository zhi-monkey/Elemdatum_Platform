
package org.dubhe.biz.base.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @description 查询模型对应训练状态
 * @date 2021-03-04
 */
@Data
@Accessors(chain = true)
public class PtModelStatusQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型id
     */
    private List<Long> modelIds;

    /**
     * 模型对应版本id
     */
    private List<Long> modelBranchIds;

}