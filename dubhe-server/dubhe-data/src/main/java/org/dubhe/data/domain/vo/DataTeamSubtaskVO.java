package org.dubhe.data.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

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
@ApiModel(value = "DataTeamTask vo", description = "子任务信息")
public class DataTeamSubtaskVO implements Serializable {

    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "子任务ID")
    private Long id;
    @ApiModelProperty(value = "任务名称")
    private String name;
    @ApiModelProperty(value = "子任务状态")
    private Integer status;
    @ApiModelProperty(value = "数据集ID")
    private Long datasetId;
    @ApiModelProperty(value = "数据集名")
    private String datasetName;
    @ApiModelProperty(value = "任务Id")
    private Long taskId;
    @ApiModelProperty(value = "起始offset")
    private Long startOffset;
    @ApiModelProperty(value = "当前offset")
    private Long currentOffset;
    @ApiModelProperty(value = "结束offset")
    private Long endOffset;
    @ApiModelProperty("创建时间")
    private Date createTime;
    @ApiModelProperty("更新时间")
    private Date updateTime;
    @ApiModelProperty(value = "任务实行者的ID")
    private Long userId;
    @ApiModelProperty(value = "任务实行者的用户名")
    private String userName;
    @ApiModelProperty(value = "任务发起者的ID")
    private Long createUserId;
    @ApiModelProperty(value = "任务发起者的用户名")
    private String createUserName;
}
