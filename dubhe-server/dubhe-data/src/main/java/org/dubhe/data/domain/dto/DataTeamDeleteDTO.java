package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
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
public class DataTeamDeleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "团队id集合")
    @NotEmpty
    private Set<Long> ids;


}
