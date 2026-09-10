

package org.dubhe.admin.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.dubhe.admin.domain.entity.Menu;
import org.dubhe.admin.domain.entity.Permission;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * @description 角色修改 DTO
 * @date 2020-06-29
 */
@Data
public class RoleUpdateDTO implements Serializable {

    private static final long serialVersionUID = -8685787591892312697L;

    private Long id;

    @Size(max = 255, message = "名称长度不能超过255")
    private String name;

    @ApiModelProperty(value = "权限id集合")
    private Set<Permission> permissions;

    /**
     * 权限
     */
    @Size(max = 255, message = "默认权限长度不能超过255")
    private String permission;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    private Set<Menu> menus;

    private Boolean deleted;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoleUpdateDTO role = (RoleUpdateDTO) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public @interface Update {
    }

}
