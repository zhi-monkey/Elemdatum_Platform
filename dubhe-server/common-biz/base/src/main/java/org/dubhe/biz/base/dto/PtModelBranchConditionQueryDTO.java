

package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @description 条件查询模型版本
 * @date 2020-03-24
 */
@Data
public class PtModelBranchConditionQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "父ID不能为空")
    private Long parentId;

    private String modelAddress;

}
