
package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @description 根据模型版本id查询模型版本详情入参
 * @date 2020-12-16
 */
@Data
public class PtModelBranchQueryByIdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型版本ID
     */
    @NotNull(message = "模型版本ID不能为空")
    private Long id;
}