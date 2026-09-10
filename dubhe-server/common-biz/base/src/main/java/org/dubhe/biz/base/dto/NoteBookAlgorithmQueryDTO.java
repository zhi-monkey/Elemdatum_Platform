

package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * @description 算法查询notebook对象
 * @date 2020-12-14
 */
@Data
public class NoteBookAlgorithmQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 算法ID
     */
    @NotEmpty(message = "算法ID不能为空")
    private List<Long> algorithmIdList;

}
