

package org.dubhe.admin.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @description 团队用户角色DTO
 * @date 2020-06-01
 */
@Data
public class TeamUserRoleSmallDTO implements Serializable {
    private static final long serialVersionUID = 6589829002637730619L;
    private Long id;
    private RoleSmallDTO role;
}
