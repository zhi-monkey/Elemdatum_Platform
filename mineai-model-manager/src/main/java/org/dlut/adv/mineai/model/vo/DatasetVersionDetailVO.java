package org.dlut.adv.mineai.model.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @description 数据集版本详细信息VO（包含图片数量、文件大小、标签数量等）
 * @date 2025-09-08
 */
@Data
public class DatasetVersionDetailVO implements Serializable {

    @ApiModelProperty("数据集版本ID")
    private Long id;

    @ApiModelProperty("数据集ID")
    private Long datasetId;

    @ApiModelProperty(value = "标注类型：2分类,1目标检测,5目标跟踪")
    private Integer annotateType;

    @ApiModelProperty("数据集名称")
    private String name;

    @ApiModelProperty("数据集版本名称")
    private String versionName;

    @ApiModelProperty("版本创建时间")
    private Date createTime;

    @ApiModelProperty("版本说明")
    private String versionNote;

    @ApiModelProperty("是否当前版本")
    private Boolean isCurrent;

    @ApiModelProperty("转换状态")
    private Integer dataConversion;

    @ApiModelProperty("是否公开")
    private short isPublic;

    @ApiModelProperty("图片数量")
    private Integer imageCount;

    @ApiModelProperty("可导入图片数量")
    private Integer importableImageCount;

    @ApiModelProperty("文件总大小（字节）")
    private Long totalFileSize;

    @ApiModelProperty("标注格式")
    private String format;

    @ApiModelProperty("标签数量统计（标签名称->数量）")
    private Map<String, Integer> labelCountMap;

}