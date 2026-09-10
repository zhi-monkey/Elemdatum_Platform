package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @description 合并文件到已有数据集入参
 * @date 2026-08-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "MergeToDataset dto", description = "合并文件到已有数据集")
public class MergeToDatasetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "目标数据集ID")
    @NotNull(message = "目标数据集不能为空")
    private Long targetDatasetId;

    @ApiModelProperty(value = "勾选的文件ID")
    @NotEmpty(message = "请至少勾选一个文件")
    private List<Long> fileIds;
}
