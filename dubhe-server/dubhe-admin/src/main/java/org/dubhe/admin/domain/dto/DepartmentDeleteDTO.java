package org.dubhe.admin.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Set;

@Data
public class DepartmentDeleteDTO implements Serializable {
    private static final long serialVersionUID = -6599428212298923816L;

    @NotEmpty(message = "部门id不能为空")
    private Set<Long> ids;
}
