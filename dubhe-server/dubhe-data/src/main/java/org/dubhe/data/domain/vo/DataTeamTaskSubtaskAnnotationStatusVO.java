package org.dubhe.data.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.vo
 * @Project：mineai
 * @name：DataTeamVO
 * @Date：2024/3/11 16:56
 * @Filename：DataTeamVO
 * @Desc：团队信息VO
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "DataTeamTaskSubtaskAnnotationStatus vo", description = "子任务标注情况")
public class DataTeamTaskSubtaskAnnotationStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "已完成数量")
    private Integer finishedCount;
    @ApiModelProperty(value = "总数量")
    private Integer totalCount;
    @ApiModelProperty(value = "子任务起始偏移量")
    private Long startOffset;
    @ApiModelProperty(value = "子任务结束偏移量")
    private Long endOffset;
    @ApiModelProperty(value = "子任务更新时间（毫秒）")
    private Long updateTime;

}
