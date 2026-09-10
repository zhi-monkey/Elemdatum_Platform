

package org.dubhe.admin.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Set;

/**
 * @description 字典详情删除DTO
 * @date 2020-06-29
 */
@Data
public class DictDetailDeleteDTO implements Serializable {

    private static final long serialVersionUID = -151060582445500836L;

    @NotEmpty(message = "id不能为空")
    private Set<Long> ids;

}
