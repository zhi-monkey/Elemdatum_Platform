
package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @description 根据模型id查询模型详情入参
 * @date 2020-12-16
 */
@Data
public class PtModelInfoQueryByIdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空")
    private Long id;
}