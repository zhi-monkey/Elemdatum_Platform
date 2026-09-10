package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * @description 从检索结果新建数据集入参
 * @date 2026-08-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "CreateDatasetFromSearch dto", description = "从检索结果新建数据集")
public class CreateDatasetFromSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "新数据集名称")
    @NotBlank(message = "数据集名称不能为空")
    private String name;

    @ApiModelProperty(value = "勾选的文件ID")
    @NotEmpty(message = "请至少勾选一个文件")
    private List<Long> fileIds;

    @ApiModelProperty(value = "数据集组ID（可选，选择已有数据集组）")
    private Long datasetGroupId;

    @ApiModelProperty(value = "数据集组名称（可选，新建数据集组）")
    private String datasetGroupName;
}
