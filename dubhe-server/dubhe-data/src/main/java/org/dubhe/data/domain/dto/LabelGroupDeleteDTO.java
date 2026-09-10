
package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @description 删除标签组
 * @date 2020-09-23
 */
@Data
public class LabelGroupDeleteDTO implements Serializable {

    @ApiModelProperty(value = "ids", required = true)
    @NotNull(message = "id不能为空")
    private Long[] ids;

}
