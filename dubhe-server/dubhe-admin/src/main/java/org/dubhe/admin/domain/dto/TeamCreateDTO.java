

package org.dubhe.admin.domain.dto;

import lombok.Data;
import org.dubhe.admin.domain.entity.User;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * @description 团队创建实体
 * @date 2020-06-29
 */
@Data
public class TeamCreateDTO implements Serializable {

    private static final long serialVersionUID = 8922409236439269071L;


    @NotBlank
    private String name;

    @NotNull
    private Boolean enabled;

    /**
     * 团队成员
     */
    private List<User> teamUserList;

    private Timestamp createTime;

}
