package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @description 文件元信息入参（可选，供数据筛选使用）
 * @date 2026-08-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "FileMetadata dto", description = "文件元信息")
public class FileMetadataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @ApiModelProperty(value = "图像质量")
    private String quality;
}
