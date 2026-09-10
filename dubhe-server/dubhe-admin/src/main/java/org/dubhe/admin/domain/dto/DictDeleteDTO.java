
package org.dubhe.admin.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Set;

/**
 * @description 字典删除DTO
 * @date 2020-06-01
 */
@Data
public class DictDeleteDTO implements Serializable {

    private static final long serialVersionUID = 6346677566514471535L;

    @NotEmpty(message = "id不能为空")
    Set<Long> ids;
}
