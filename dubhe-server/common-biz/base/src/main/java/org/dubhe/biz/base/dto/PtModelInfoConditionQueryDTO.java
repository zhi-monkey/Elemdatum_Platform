
package org.dubhe.biz.base.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.base.utils.PtModelUtil;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;

/**
 * @description 模型条件查询
 * @date 2020-12-17
 */
@Data
@Accessors
public class PtModelInfoConditionQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型是否为预置模型（0默认模型，1预置模型）
     *
     */
    @Min(value = PtModelUtil.NUMBER_ZERO, message = "模型类型错误")
    @Max(value = PtModelUtil.NUMBER_TWO, message = "模型类型错误")
    @NotNull(message = "modelResource不能为空")
    private Integer modelResource;

    /**
     * 模型id
     */
    @NotNull(message = "id不能为空")
    private Set<Long> ids;

}