package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.entity
 * @Project：mineai
 * @name：TeamUser
 * @Date：2024/3/11 15:49
 * @Filename：DataTeamTaskSubtask
 * @Desc：任务-子任务关联
 */

@Data
@TableName("data_team_task_subtask")
public class DataTeamTaskSubtask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableField(value = "task_id")
    private Long taskId;

    @TableField(value = "subtask_id")
    private Long subtaskId;
}
