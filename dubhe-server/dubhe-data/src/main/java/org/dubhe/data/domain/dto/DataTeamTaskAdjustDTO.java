package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.dto
 * @Project：mineai
 * @name：DataTeamTaskAdjustDTO
 * @Date：2025/10/15
 * @Filename：DataTeamTaskAdjustDTO
 * @Desc：调整任务比例DTO
 */
@Data
@ApiModel("任务更新DTO")
public class DataTeamTaskAdjustDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "任务ID", required = true)
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @ApiModelProperty(value = "成员分配数量")
    @Valid
    private Map<Long, Integer> userAllocations;

}