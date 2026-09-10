
package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @description 根据名称查询字典详情
 * @date 2020-12-23
 */
@Data
public class DictDetailQueryByLabelNameDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Label名称不能为空")
    private String name;
}