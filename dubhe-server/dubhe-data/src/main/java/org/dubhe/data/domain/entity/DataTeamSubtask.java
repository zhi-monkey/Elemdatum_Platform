package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
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
 * @Desc：标注子任务实体类
 */

@Data
//@EqualsAndHashCode(callSuper = false)
//@Accessors(chain = true)
@TableName("data_team_subtask")
@ApiModel(value = "标注子任务实体类", description = "多人标注")
@Builder
//@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DataTeamSubtask extends BaseEntity {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer status;
    @TableField(value = "user_id")
    private Long userId;
    @TableField(value = "dataset_id")
    private Long datasetId;
    @TableField(value = "task_id")
    private Long taskId;
    @TableField(value = "start_offset")
    private Long startOffset;
    @TableField(value = "current_offset")
    private Long currentOffset;
    @TableField(value = "end_offset")
    private Long endOffset;
    @TableField(value = "deleted",fill = FieldFill.INSERT)
    private Boolean deleted;
    @TableField(value = "create_user_id",fill = FieldFill.INSERT)
    private Long createUserId;


}
