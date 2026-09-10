
package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @description 根据Id查询
 * @date 2020-12-23
 */
@Data
public class TrainAlgorithmSelectByIdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "id不能为空")
    private Long id;
}