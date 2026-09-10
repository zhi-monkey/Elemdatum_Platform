package org.dubhe.admin.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Mayn
 */
@Data
public class RoleUserCountDTO implements Serializable {
    private String roleName;  // 角色名
    private Integer userCount;  // 该角色的人数
}
