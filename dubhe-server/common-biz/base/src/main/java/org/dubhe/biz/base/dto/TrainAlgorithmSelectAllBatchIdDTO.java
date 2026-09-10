
package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;

/**
 * @description 根据Id批量查询
 * @date 2020-12-23
 */
@Data
public class TrainAlgorithmSelectAllBatchIdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "id不能为空")
    private Set<Long> ids;
}