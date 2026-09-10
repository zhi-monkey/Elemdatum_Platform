
package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @description 根据Id查询所有数据(包含已被软删除的数据)
 * @date 2020-12-23
 */
@Data
public class TrainAlgorithmSelectAllByIdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "id不能为空")
    private Long id;
}