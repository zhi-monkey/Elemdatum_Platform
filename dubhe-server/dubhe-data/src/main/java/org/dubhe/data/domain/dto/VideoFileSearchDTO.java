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
 * @description 视频文件检索入参
 * @date 2026-08-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "VideoFileSearch dto", description = "视频文件检索")
public class VideoFileSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "数据集ID（多选，可选）")
    private List<Long> datasetIds;

    @ApiModelProperty(value = "文件名（前缀匹配）")
    private String name;

    @ApiModelProperty(value = "更新时间起始（yyyy-MM-dd HH:mm:ss）")
    private String updateTimeStart;

    @ApiModelProperty(value = "更新时间截止（yyyy-MM-dd HH:mm:ss）")
    private String updateTimeEnd;

    @ApiModelProperty(value = "当前页（1起）")
    private Long current;

    @ApiModelProperty(value = "每页条数")
    private Long size;

    @ApiModelProperty(value = "排序字段：id/name/updateTime/fileSize")
    private String sortField;

    @ApiModelProperty(value = "排序方式：asc/desc")
    private String sortOrder;
}
