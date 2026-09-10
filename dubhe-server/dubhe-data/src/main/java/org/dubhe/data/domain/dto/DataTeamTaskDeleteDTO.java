package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Set;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.dto
 * @Project：mineai
 * @name：DataTeamQueryDTO
 * @Date：2024/3/11 16:21
 * @Filename：DataTeamQueryDTO
 * @Desc：
 */

@Data
public class DataTeamTaskDeleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "任务id集合")
    @NotEmpty
    private Set<Long> ids;


}
