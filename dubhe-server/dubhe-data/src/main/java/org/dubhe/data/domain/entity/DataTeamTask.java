package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dubhe.biz.db.entity.BaseEntity;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.entity
 * @Project：mineai
 * @name：Team
 * @Date：2024/3/11 15:37
 * @Filename：Team
 * @Desc：标注任务实体类
 */

@Data
//@EqualsAndHashCode(callSuper = false)
//@Accessors(chain = true)
@TableName("data_team_task")
@ApiModel(value = "标注任务实体类", description = "多人标注")
@Builder
//@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DataTeamTask extends BaseEntity {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer status;
    private Integer strategy;
    @TableField(value = "dataset_id")
    private Long datasetId;
    @TableField(value = "dataset_name")
    private String datasetName;
    @TableField(value = "dataset_type")
    private Integer datasetType;
    @TableField(value = "team_id")
    private Long teamId;
    @TableField(value = "team_name")
    private String teamName;
    @TableField(value = "deleted",fill = FieldFill.INSERT)
    private Boolean deleted;



}
