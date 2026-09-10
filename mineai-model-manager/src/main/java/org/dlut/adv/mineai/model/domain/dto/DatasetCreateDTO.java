package org.dlut.adv.mineai.model.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @description 数据集
 * @date 2020-04-17
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DatasetCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String remark;

    private Integer type;

    private Long teamId;

    private Integer dataType;

    private Integer annotateType;

    @ApiModelProperty(notes = "标签组Id")
    private Long labelGroupId;

    @ApiModelProperty(value = "预置标签类型 2:imageNet  3:MS COCO")
    private Integer presetLabelType;

    @ApiModelProperty(value = "是否用户导入")
    @TableField(value = "is_import")
    private boolean isImport;

    @ApiModelProperty(value = "模板")
    private Integer templateType;

    @ApiModelProperty(value = "所属模块")
    private Integer module;


    @ApiModelProperty(value = "是否公开")
    private short isPublic;

    @ApiModelProperty(value = "是否是引导式：0不是 1是")
    private Boolean isGuided = false;

    @ApiModelProperty(value = "数据集分组名称")
    private String datasetGroupName;

    @ApiModelProperty(value = "是否需要新增数据集组")
    private Boolean isCreateDatasetGroup;

    public @interface Create {
    }

}
