

package org.dubhe.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;

/**
 * @description 团队实体
 * @date 2020-06-29
 */
@TableName("team")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    @TableId(value = "id", type = IdType.AUTO)
    @NotNull(groups = Update.class)
    private Long id;

    @TableField(value = "name")
    @NotBlank
    private String name;

    @TableField(value = "enabled")
    @NotNull
    private Boolean enabled;

    /**
     * 团队成员
     */
    @TableField(exist = false)
    private List<User> teamUserList;

    @TableField(value = "create_time")
    private Timestamp createTime;


    public @interface Update {
    }
}
