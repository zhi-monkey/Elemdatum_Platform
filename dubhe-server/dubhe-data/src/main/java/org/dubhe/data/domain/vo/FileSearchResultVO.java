package org.dubhe.data.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @description 图片多条件检索结果
 * @date 2026-08-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "FileSearchResult vo", description = "图片多条件检索结果")
public class FileSearchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "文件ID")
    private Long id;

    @ApiModelProperty(value = "文件名")
    private String name;

    @ApiModelProperty(value = "资源访问路径")
    private String url;

    @ApiModelProperty(value = "图片宽")
    private Integer width;

    @ApiModelProperty(value = "图片高")
    private Integer height;

    @ApiModelProperty(value = "文件类型")
    private Integer fileType;

    @ApiModelProperty(value = "数据集ID")
    private Long datasetId;

    @ApiModelProperty(value = "数据集名称")
    private String datasetName;

    @ApiModelProperty(value = "标注状态")
    private Integer annotationStatus;

    @ApiModelProperty(value = "标签（逗号分隔）")
    private String labels;

    @ApiModelProperty(value = "更新时间")
    private String updateTime;

    @ApiModelProperty(value = "数据来源")
    private String sourceType;

    @ApiModelProperty(value = "采集时间")
    private String captureTime;

    @ApiModelProperty(value = "车辆或设备")
    private String device;

    @ApiModelProperty(value = "设备或相机编号")
    private String deviceSn;

    @ApiModelProperty(value = "采集地点")
    private String location;

    @ApiModelProperty(value = "业务场景")
    private String scenario;

    @ApiModelProperty(value = "光照或环境条件")
    private String lighting;

    @ApiModelProperty(value = "数据质量")
    private String quality;
}
