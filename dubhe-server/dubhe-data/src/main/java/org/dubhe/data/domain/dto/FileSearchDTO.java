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
 * @description 图片多条件检索入参
 * @date 2026-08-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "FileSearch dto", description = "图片多条件检索")
public class FileSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "数据集ID（多选，可选）")
    private List<Long> datasetIds;

    @ApiModelProperty(value = "文件ID（多选，可选，导出勾选用）")
    private List<Long> fileIds;

    @ApiModelProperty(value = "数据集组ID（可选）")
    private Long datasetGroupId;

    @ApiModelProperty(value = "文件名（前缀匹配）")
    private String name;

    @ApiModelProperty(value = "最小宽度")
    private Integer minWidth;

    @ApiModelProperty(value = "最大宽度")
    private Integer maxWidth;

    @ApiModelProperty(value = "最小高度")
    private Integer minHeight;

    @ApiModelProperty(value = "最大高度")
    private Integer maxHeight;

    @ApiModelProperty(value = "更新时间起始（yyyy-MM-dd HH:mm:ss）")
    private String updateTimeStart;

    @ApiModelProperty(value = "更新时间截止（yyyy-MM-dd HH:mm:ss）")
    private String updateTimeEnd;

    @ApiModelProperty(value = "标注状态（多选）101未标注 102标注中 103自动标注完成 104已标注")
    private List<Integer> annotationStatus;

    @ApiModelProperty(value = "标签ID（多选，ANY语义）")
    private List<Long> labelIds;

    @ApiModelProperty(value = "标签名（多选，从标签库 label_template 选择，AND 语义）")
    private List<String> labelNames;

    @ApiModelProperty(value = "数据来源（多选）")
    private List<String> sourceType;

    @ApiModelProperty(value = "采集时间起始（yyyy-MM-dd HH:mm:ss）")
    private String captureTimeStart;

    @ApiModelProperty(value = "采集时间截止（yyyy-MM-dd HH:mm:ss）")
    private String captureTimeEnd;

    @ApiModelProperty(value = "车辆或设备（多选）")
    private List<String> device;

    @ApiModelProperty(value = "设备或相机编号")
    private String deviceSn;

    @ApiModelProperty(value = "采集地点（多选）")
    private List<String> location;

    @ApiModelProperty(value = "业务场景（多选）")
    private List<String> scenario;

    @ApiModelProperty(value = "光照或环境条件（多选）")
    private List<String> lighting;

    @ApiModelProperty(value = "数据质量（多选）")
    private List<String> quality;

    @ApiModelProperty(value = "当前页（1起）")
    private Long current;

    @ApiModelProperty(value = "每页条数")
    private Long size;

    @ApiModelProperty(value = "排序字段：id/name/updateTime/captureTime")
    private String sortField;

    @ApiModelProperty(value = "排序方式：asc/desc")
    private String sortOrder;
}
