package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description 文件元信息（供数据筛选使用）
 * @date 2026-08-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_file_metadata")
@ApiModel(value = "FileMetadata对象", description = "文件元信息")
public class FileMetadata {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "文件ID，关联 data_file.id")
    private Long fileId;

    @ApiModelProperty(value = "数据集ID")
    private Long datasetId;

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
