package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @description 数据批量导出任务入参
 * @date 2026-08-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "ExportTaskCreate dto", description = "数据批量导出任务")
public class ExportTaskCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "检索条件（可选，与 fileIds 二选一或同时为空则导出全部）")
    private FileSearchDTO condition;

    @ApiModelProperty(value = "勾选的文件ID（可选）")
    private List<Long> fileIds;

    @ApiModelProperty(value = "导出格式：csv/json/zip")
    private String format;
}
