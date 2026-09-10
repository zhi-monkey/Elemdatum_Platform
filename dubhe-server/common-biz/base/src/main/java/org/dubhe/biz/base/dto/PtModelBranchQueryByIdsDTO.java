
package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
public class PtModelBranchQueryByIdsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型版本ID
     */
    @NotEmpty(message = "模型版本ID列表不能为空")
    private List<Long> ids;
}
