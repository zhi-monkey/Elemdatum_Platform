package org.dlut.adv.mineai.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.dlut.adv.mineai.model.vo.DatasetVersionKey;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@ApiModel("数据集合并请求参数")
public class DatasetMergeRequest {

    @ApiModelProperty(value = "要合并的数据集版本列表", required = true)
    @NotEmpty(message = "版本列表不能为空")
    private List<Long> versions;

    @ApiModelProperty(value = "目标输出目录", required = true, example = "merged/dataset_20240101")
    @NotBlank(message = "目标目录不能为空")
    private String targetDir;

    @ApiModelProperty(value = "合并描述", example = "合并训练集和验证集")
    private String description;

    @ApiModelProperty(value = "需要保留的标签列表", required = true)
    private List<String> keepLabels;

    @ApiModelProperty(value = "是否切分")
    private Boolean isSplit = false;

    @ApiModelProperty(value = "切分比例（训练-验证-测试）", example = "80-10-10")
    private String splitRatio = "80-10-10";

}
