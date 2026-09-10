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
 * @Filename：TeamUser
 * @Desc：团队-用户关联
 */

@Data
@TableName("data_team_user")
public class DataTeamUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableField(value = "data_team_id")
    private Long dataTeamId;

    @TableField(value = "user_id")
    private Long userId;
}
