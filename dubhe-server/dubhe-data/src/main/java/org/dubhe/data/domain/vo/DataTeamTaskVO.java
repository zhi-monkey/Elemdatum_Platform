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
@ApiModel(value = "DataTeamTask vo", description = "任务信息")
public class DataTeamTaskVO implements Serializable {

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
    @ApiModelProperty(value = "数据集状态")
    private Integer datasetType;
    @ApiModelProperty(value = "团队名称")
    private String teamName;
    @ApiModelProperty("创建时间")
    private Date createTime;
}
