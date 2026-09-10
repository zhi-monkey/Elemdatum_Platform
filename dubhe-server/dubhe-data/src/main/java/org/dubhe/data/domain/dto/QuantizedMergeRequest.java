package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author 10230
 */
@Data
@ApiModel("数据集合并请求参数")
public class QuantizedMergeRequest {

    @ApiModelProperty(value = "要合并的数据集版本列表", required = true)
    @NotEmpty(message = "版本列表不能为空")
    private List<Long> versions;

    @ApiModelProperty(value = "目标输出目录", required = true, example = "merged/dataset_20240101")
    @NotBlank(message = "目标目录不能为空")
    private String targetDir;


    @ApiModelProperty(value = "图片总数")
    private Long total = 1L;

}