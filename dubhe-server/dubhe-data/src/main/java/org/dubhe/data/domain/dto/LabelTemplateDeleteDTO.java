package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class LabelTemplateDeleteDTO implements Serializable {

    @ApiModelProperty(value = "ids", required = true)
    @NotNull(message = "id不能为空")
    private Long[] ids;

}
