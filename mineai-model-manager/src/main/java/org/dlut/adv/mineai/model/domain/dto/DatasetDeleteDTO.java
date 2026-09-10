package org.dlut.adv.mineai.model.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 *
 * @author 10230
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DatasetDeleteDTO implements Serializable {

    @ApiModelProperty(value = "ids", required = true)
    @NotNull(message = "id不能为空")
    private Long[] ids;

    public DatasetDeleteDTO(Long trainDatasetStart) {
        this.ids = new Long[]{trainDatasetStart};
    }

    public @interface Create {
    }

}
