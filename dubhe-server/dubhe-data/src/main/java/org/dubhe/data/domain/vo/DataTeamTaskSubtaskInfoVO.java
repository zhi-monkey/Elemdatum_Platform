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
@ApiModel(value = "DataTeamTaskSubtaskInfo vo", description = "任务以及相关子任务信息")
public class DataTeamTaskSubtaskInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "任务ID")
    private Long id;
    @ApiModelProperty(value = "任务名称")
    private String name;
    @ApiModelProperty(value = "任务状态")
    private Integer status;
    @ApiModelProperty(value = "数据集ID")
    private Long datasetId;
    @ApiModelProperty(value = "数据集名称")
    private String datasetName;
    @ApiModelProperty(value = "团队名称")
    private String teamName;
    @ApiModelProperty(value = "团队ID")
    private Long teamId;
    @ApiModelProperty(value = "团队人数")
    private Integer memberNum;
    @ApiModelProperty(value = "图片数量")
    private Integer imageCount;
    @ApiModelProperty(value = "任务图片数量（实际分配的图片数）")
    private Integer taskImageCount;
    @ApiModelProperty(value = "每人数量")
    private Integer personCount;
    @ApiModelProperty(value = "已完成数量")
    private Integer finishedCount;
    @ApiModelProperty(value = "完成度")
    private Integer progress;
    @ApiModelProperty(value = "子任务列表")
    private List<DataTeamSubtaskVO> subtaskVOList;


}
