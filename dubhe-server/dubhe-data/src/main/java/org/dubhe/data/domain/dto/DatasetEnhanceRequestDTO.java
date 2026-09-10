

package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.dubhe.biz.base.constant.NumberConstant;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;


/**
 * @description 数据增强请求 DTO
 * @date 2020-06-29
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DatasetEnhanceRequestDTO implements Serializable {

    @ApiModelProperty(value = "数据集ID")
    @NotNull(message = "数据集id不能为空")
    @Min(value = NumberConstant.NUMBER_0, message = "数据集id不能为负")
    private Long datasetId;

    @ApiModelProperty(value = "数据增强类型 1.去雾 2.增雾 3.对比度增强 4.直方图均衡化")
    @NotNull(message = "增强类型不能为空")
    private List<Integer> types;


    @ApiModelProperty(value = "任务参数(JSON字符串)")
    private String taskParams;

    @ApiModelProperty(value = "增强数量，代表期望获得的新图片数")
    @NotNull(message = "增强数量不能为空")
    @Min(value = 1, message = "增强数量必须大于0")
    private Integer enhanceCount;

}
