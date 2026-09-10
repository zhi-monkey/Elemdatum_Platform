package org.dubhe.data.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @description 数据导出任务结果
 * @date 2026-08-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "ExportTask vo", description = "数据导出任务结果")
public class ExportTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "任务ID")
    private Long id;

    @ApiModelProperty(value = "任务号")
    private String taskId;

    @ApiModelProperty(value = "导出格式")
    private String format;

    @ApiModelProperty(value = "状态 PENDING/PROCESSING/COMPLETED/FAILED/CANCELLED")
    private String status;

    @ApiModelProperty(value = "进度 0-100")
    private Integer progress;

    @ApiModelProperty(value = "导出文件相对路径")
    private String resultUrl;

    @ApiModelProperty(value = "错误信息")
    private String errorMessage;

    @ApiModelProperty(value = "数据条数")
    private Integer fileCount;

    @ApiModelProperty(value = "导出人")
    private String createUserName;

    @ApiModelProperty(value = "过期时间")
    private String expireTime;

    @ApiModelProperty(value = "执行日志")
    private String log;

    @ApiModelProperty(value = "数据集统计")
    private String datasetSummary;

    @ApiModelProperty(value = "筛选条件摘要")
    private String conditionSummary;

    @ApiModelProperty(value = "创建时间")
    private String createTime;
}
